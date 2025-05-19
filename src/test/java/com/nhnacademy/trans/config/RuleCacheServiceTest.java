package com.nhnacademy.trans.config;

import com.nhnacademy.trans.adaptor.RuleAdaptor;
import com.nhnacademy.trans.domain.RuleCache;
import com.nhnacademy.trans.domain.Threshold;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleCacheServiceTest {

    @Mock
    private RedisTemplate<String, Threshold> redisTemplate;

    @Mock
    private RuleAdaptor ruleAdaptor;

    @InjectMocks
    private RuleCacheService ruleCacheService;

    @Test
    void testReloadAllRules_success() {
        var hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        Map<String, Threshold> sensorRulesMap = new HashMap<>();
        sensorRulesMap.put("temp", new Threshold(0, 100));
        RuleCache sensorCache = new RuleCache("sensor1", "domain1", sensorRulesMap);

        Map<String, Threshold> serverRulesMap = new HashMap<>();
        serverRulesMap.put("cpu", new Threshold(1, 5));
        RuleCache serverCache = new RuleCache("server1", "domain2", serverRulesMap);

        when(ruleAdaptor.findAllSensorData())
                .thenReturn(ResponseEntity.ok(List.of(sensorCache)));
        when(ruleAdaptor.findAllServerData())
                .thenReturn(ResponseEntity.ok(List.of(serverCache)));


        ruleCacheService.reloadAllRules();

        verify(hashOps).putAll("rule:domain1:sensor1", sensorRulesMap);
        verify(hashOps).putAll("rule:domain2:server1", serverRulesMap);
    }

    @Test
    void testReloadAllRules_failure_sensorApi() {
        // given: sensor API error
        when(ruleAdaptor.findAllSensorData())
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // when & then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ruleCacheService.reloadAllRules());
        assertTrue(ex.getMessage().contains("룰 API 호출 실패"));
        verify(ruleAdaptor, never()).findAllServerData();
        verify(redisTemplate, never()).opsForHash();
    }

    @Test
    void testGetRulesFromRedis_returnsEntries() {
        // stub opsForHash for entries
        var hashOps = mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        Map<String, Threshold> expected = Map.of("h", new Threshold(10, 20));
        when(hashOps.entries("rule:server:domainX:idX")).thenReturn(expected);

        // when
        Map<String, Threshold> result = ruleCacheService.getRulesFromRedis(
                "server", "domainX", "idX");

        // then
        assertSame(expected, result);
    }

    @Test
    void testGetThreshold_present() {
        // stub opsForHash for get
        var hashOps = mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        Threshold threshold = new Threshold(5, 15);
        when(hashOps.get("rule:domainY:sensorY", "humidity")).thenReturn(threshold);

        // when
        Optional<Threshold> result = ruleCacheService.getThreshold(
                "humidity", "domainY", "sensorY");

        // then
        assertTrue(result.isPresent());
        assertEquals(threshold, result.get());
    }

    @Test
    void testGetThreshold_absent() {
        // stub opsForHash for get
        var hashOps = mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get("rule:domainZ:serverZ", "pressure")).thenReturn(null);

        // when
        Optional<Threshold> result = ruleCacheService.getThreshold(
                "pressure", "domainZ", "serverZ");

        // then
        assertTrue(result.isEmpty());
    }
}
