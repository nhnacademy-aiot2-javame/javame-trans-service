package com.nhnacademy.trans.config;

import com.nhnacademy.trans.adaptor.RuleAdaptor;
import com.nhnacademy.trans.domain.RuleCache;
import com.nhnacademy.trans.domain.ServerData;
import com.nhnacademy.trans.domain.Threshold;
import com.nhnacademy.trans.repository.SensorDataRepository;
import com.nhnacademy.trans.repository.ServerDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Supplier;

@Service
public class RuleCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Threshold> hashOperations;
    private RuleAdaptor ruleAdaptor;

    @Autowired
    public RuleCacheService(
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    /** MySQL에서 룰을 가져와 Redis에 저장 */
    /**
     * 센서 및 서버 룰을 Redis에 일괄 로드
     */
    public void reloadAllRules() {
        List<RuleCache> sensorRules = fetchRules(() -> ruleAdaptor.findAllSensorData(), "sensor");
        List<RuleCache> serverRules = fetchRules(() -> ruleAdaptor.findAllServerData(), "server");

        cacheRules("sensor", sensorRules);
        cacheRules("server", serverRules);
    }

    /**
     * Feign 호출 및 응답 검증
     */
    private List<RuleCache> fetchRules(Supplier<ResponseEntity<List<RuleCache>>> supplier, String type) {
        ResponseEntity<List<RuleCache>> response = supplier.get();
        if (response.getStatusCode().is2xxSuccessful()) {
            return Objects.requireNonNull(response.getBody());
        }
        throw new RuntimeException(type + " 룰 API 호출 실패: " + response.getStatusCode());
    }

    /**
     * Redis에 룰 저장
     */
    private void cacheRules(String type, List<RuleCache> rules) {
        String keyPrefix = "rule:" + type + ":";
        for (RuleCache ruleCache : rules) {
            String key = keyPrefix + ruleCache.getId();
            hashOperations.putAll(key, ruleCache.getRules());
        }
    }



    /** Redis에서 룰 조회 */
    public Map<String, Threshold> getRulesFromRedis(String type, String id) {
        String key = "rule:" + type + ":" + id;
        return hashOperations.entries(key);
    }
}
