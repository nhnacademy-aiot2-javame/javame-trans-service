package com.nhnacademy.trans.config;

import com.nhnacademy.trans.adaptor.RuleAdaptor;
import com.nhnacademy.trans.domain.RuleCache;
import com.nhnacademy.trans.domain.Threshold;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RuleCacheService {

    /**
     * RedisTemplate 의존성 주입
     */
    private final RedisTemplate<String, Threshold> redisTemplate;

    /**
     * Javame-Rule-Api 를 @FeignClient 로 가져옴
     */
    private final RuleAdaptor ruleAdaptor;

    /**
     * MySQL에서 센서 및 서버 룰을 모두 가져와 Redis에 저장
     * <code>fetchRules</code>
     */
    public void reloadAllRules() {
        List<RuleCache> sensorRules = fetchRules(ruleAdaptor::findAllSensorData);
        List<RuleCache> serverRules = fetchRules(ruleAdaptor::findAllServerData);

        cacheRules(sensorRules);
        cacheRules(serverRules);
    }

    /**
     * Feign 호출 및 응답 검증
     * @param supplier 다른 메서드의 중복 코드를 일괄 처리하는 인터페이스
     */
    private List<RuleCache> fetchRules(Supplier<ResponseEntity<List<RuleCache>>> supplier) {
        ResponseEntity<List<RuleCache>> response = supplier.get();
        if (response.getStatusCode().is2xxSuccessful()) {
            return Objects.requireNonNull(response.getBody());
        }
        throw new RuntimeException("룰 API 호출 실패: " + response.getStatusCode());
    }

    /**
     * Redis에 룰 저장 (키 패턴: rule:{domain}:{id})
     * @param rules : 각 센서나 서버 measurement 의 임계값
     */
    private void cacheRules(List<RuleCache> rules) {
        for (RuleCache ruleCache : rules) {
            String key = String.format("rule:%s:%s",
                    ruleCache.getCompanyDomain(),
                    ruleCache.getId());
            // RedisTemplate의 HashOperations 사용
            redisTemplate.opsForHash().putAll(key, ruleCache.getRules());
        }
    }

    /**
     * Redis 에서 룰 전체 조회
     * @param type   룰 맵의 키(예: "temperature", "humidity")
     * @param domain companyDomain
     * @param id     sensorId 또는 serverId
     * @return       Map<ruleKey, Threshold>
     */
    public Map<String, Threshold> getRulesFromRedis(String type, String domain, String id) {
        String key = String.format("rule:%s:%s:%s", type, domain, id);
        return redisTemplate.<String, Threshold>opsForHash().entries(key);
    }

    /**
     * Redis 에서 특정 룰(Threshold) 조회
     * @param type   룰 맵의 키(예: "temperature", "humidity")
     * @param domain companyDomain
     * @param id     sensorId 또는 serverId
     * @return       Threshold 또는 Optional.empty()
     */
    public Optional<Threshold> getThreshold(String type, String domain, String id) {
        String key = String.format("rule:%s:%s", domain, id);
        Threshold threshold = redisTemplate.<String, Threshold>opsForHash().get(key, type);
        return Optional.ofNullable(threshold);
    }
}
