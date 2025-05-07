package com.nhnacademy.trans.config;

import com.nhnacademy.trans.adaptor.RuleAdaptor;
import com.nhnacademy.trans.domain.RuleCache;
import com.nhnacademy.trans.domain.Threshold;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
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

    private final HashOperations<String, String, Threshold> hashOperations;
    private final RuleAdaptor ruleAdaptor;

    /**
     * MySQL에서 센서 및 서버 룰을 모두 가져와 Redis에 저장
     */
    public void reloadAllRules() {
        List<RuleCache> sensorRules = fetchRules(ruleAdaptor::findAllSensorData, "sensor");
        List<RuleCache> serverRules = fetchRules(ruleAdaptor::findAllServerData, "server");

        cacheRules(sensorRules);
        cacheRules(serverRules);
    }

    /**
     * Feign 호출 및 응답 검증
     */
    private List<RuleCache> fetchRules(Supplier<ResponseEntity<List<RuleCache>>> supplier,
                                       String type) {
        var response = supplier.get();
        if (response.getStatusCode().is2xxSuccessful()) {
            return Objects.requireNonNull(response.getBody());
        }
        throw new RuntimeException(type + " 룰 API 호출 실패: " + response.getStatusCode());
    }

    /**
     * Redis에 룰 저장 (키 패턴: rule:{type}:{domain}:{id})
     */
    private void cacheRules(List<RuleCache> rules) {
        for (RuleCache ruleCache : rules) {
            String key = String.format("rule:%s:%s",
                    ruleCache.getCompanyDomain(),
                    ruleCache.getId());
            hashOperations.putAll(key, ruleCache.getRules());
        }
    }

    /**
     * Redis에서 룰 전체 조회
     * @param type    "sensor" 또는 "server"
     * @param domain  companyDomain
     * @param id      sensorId 또는 serverId
     * @return        Map<ruleKey, Threshold>
     */
    public Map<String, Threshold> getRulesFromRedis(String type, String domain, String id) {
        String key = String.format("rule:%s:%s:%s", type, domain, id);
        return hashOperations.entries(key);
    }

    /**
     * Redis에서 특정 룰(Threshold) 조회
     * @param type     룰 맵의 키(예: "temperature", "humidity")
     * @param domain   companyDomainZ
     * @param id       sensorId 또는 serverId

     * @return         Threshold 또는 Optional.empty()
     */
    public Optional<Threshold> getThreshold(String type, String domain, String id) {
        String key = String.format("rule:%s:%s", domain, id);
        return Optional.ofNullable(hashOperations.get(key, type));
    }
}
