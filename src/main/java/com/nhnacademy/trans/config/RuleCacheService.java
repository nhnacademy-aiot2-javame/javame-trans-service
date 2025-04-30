package com.nhnacademy.trans.config;

import com.nhnacademy.trans.domain.RuleCache;
import com.nhnacademy.trans.domain.ServerData;
import com.nhnacademy.trans.domain.Threshold;
import com.nhnacademy.trans.repository.SensorDataRepository;
import com.nhnacademy.trans.repository.ServerDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class RuleCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Threshold> hashOperations;

    @Autowired
    public RuleCacheService(
            SensorDataRepository sensorDataRepository,
            ServerDataRepository serverDataRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.sensorDataRepository = sensorDataRepository;
        this.serverDataRepository = serverDataRepository;
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    /** MySQL에서 룰을 가져와 Redis에 저장 */
    @Transactional
    public void loadRulesToRedis() {
        List<RuleCache> sensorRules = loadSensorRules();
        List<RuleCache> serverRules = loadServerRules();

        // Redis에 저장
        for (RuleCache ruleCache : sensorRules) {
            String key = "rule:sensor:" + ruleCache.getId();
            hashOperations.putAll(key, ruleCache.getRules());
        }
        for (RuleCache ruleCache : serverRules) {
            String key = "rule:server:" + ruleCache.getId();
            hashOperations.putAll(key, ruleCache.getRules());
        }
    }

    /** MySQL에서 센서별 룰을 조회 */
    private List<RuleCache> loadSensorRules() {
        List<DataType> dataTypes = sensorDataRepository.findAll();
        Map<Long, List<DataType>> sensorMap = new HashMap<>();

        for (DataType dt : dataTypes) {
            sensorMap.computeIfAbsent(dt.getSensorNumber(), k -> new ArrayList<>()).add(dt);
        }

        List<RuleCache> ruleCaches = new ArrayList<>();
        for (Map.Entry<Long, List<DataType>> entry : sensorMap.entrySet()) {
            Map<String, Threshold> ruleMap = new HashMap<>();
            for (DataType dt : entry.getValue()) {
                ruleMap.put(dt.getDataTypeName(), new Threshold(dt.getMinThreshold(), dt.getMaxThreshold()));
            }
            ruleCaches.add(new RuleCache(entry.getKey().toString(), "sensor", ruleMap));
        }
        return ruleCaches;
    }

    /** MySQL에서 서버별 룰을 조회 */
    private List<RuleCache> loadServerRules() {
        List<ServerData> serverDataList = serverDataRepository.findAll();
        Map<Long, List<ServerData>> serverMap = new HashMap<>();

        for (ServerData sd : serverDataList) {
            serverMap.computeIfAbsent(sd.getServerNo(), k -> new ArrayList<>()).add(sd);
        }

        List<RuleCache> ruleCaches = new ArrayList<>();
        for (Map.Entry<Long, List<ServerData>> entry : serverMap.entrySet()) {
            Map<String, Threshold> ruleMap = new HashMap<>();
            for (ServerData sd : entry.getValue()) {
                ruleMap.put(sd.getServerDataTopic(), new Threshold(sd.getMinThreshold(), sd.getMaxThreshold()));
            }
            ruleCaches.add(new RuleCache(entry.getKey().toString(), "server", ruleMap));
        }
        return ruleCaches;
    }

    /** Redis에서 룰 조회 */
    public Map<String, Threshold> getRulesFromRedis(String type, String id) {
        String key = "rule:" + type + ":" + id;
        return hashOperations.entries(key);
    }
}
