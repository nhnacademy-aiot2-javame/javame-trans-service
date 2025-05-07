package com.nhnacademy.trans.config;

import com.nhnacademy.trans.adaptor.RuleAdaptor;
import com.nhnacademy.trans.domain.RuleCache;
import com.nhnacademy.trans.domain.Threshold;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private HashOperations<String, String, Threshold> hashOperations;

    @Mock
    private RuleAdaptor ruleAdaptor;

    @InjectMocks
    private RuleCacheService ruleCacheService;

    @Test
    void testReloadAllRules_success() {
        // given
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

        // when
        ruleCacheService.reloadAllRules();

        // then
        verify(hashOperations).putAll("rule:domain1:sensor1", sensorRulesMap);
        verify(hashOperations).putAll("rule:domain2:server1", serverRulesMap);
    }

    @Test
    void testReloadAllRules_failure_sensorApi() {
        // given: sensor API returns 500
        when(ruleAdaptor.findAllSensorData())
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> ruleCacheService.reloadAllRules());
        assertTrue(exception.getMessage().contains("sensor 룰 API 호출 실패"));
        // server API should not be invoked
        verify(ruleAdaptor, never()).findAllServerData();
    }

    @Test
    void testGetRulesFromRedis_returnsEntries() {
        // given
        Map<String, Threshold> expected = Map.of("h", new Threshold(10, 20));
        when(hashOperations.entries("rule:server:domainX:idX")).thenReturn(expected);

        // when
        Map<String, Threshold> result = ruleCacheService.getRulesFromRedis("server", "domainX", "idX");

        // then
        assertSame(expected, result);
    }

    @Test
    void testGetThreshold_present() {
        // given
        Threshold threshold = new Threshold(5, 15);
        when(hashOperations.get("rule:domainY:sensorY", "humidity"))
                .thenReturn(threshold);

        // when
        Optional<Threshold> result = ruleCacheService.getThreshold( "humidity", "domainY", "sensorY");

        // then
        assertTrue(result.isPresent());
        assertEquals(threshold, result.get());
    }

    @Test
    void testGetThreshold_absent() {
        // given
        when(hashOperations.get("rule:domainZ:serverZ", "pressure"))
                .thenReturn(null);

        // when
        Optional<Threshold> result = ruleCacheService.getThreshold("pressure","domainZ", "serverZ");

        // then
        assertTrue(result.isEmpty());
    }
}
