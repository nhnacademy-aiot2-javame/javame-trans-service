package com.nhnacademy.trans;

import com.nhnacademy.trans.config.RuleCacheService;
import com.nhnacademy.trans.domain.Threshold;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 룰(임계값) 조회를 담당하는 Cache Manager
 */
@Component
@RequiredArgsConstructor
public class ThresholdCacheManager {

    private final RuleCacheService ruleCacheService;

    /**
     * Redis에서 특정 센서/서버의 임계값을 조회
     *
     * @param type      룰 맵의 키 (예: "temperature", "humidity")
     * @param domain    companyDomain
     * @param id        sensorId 또는 serverId
     * @return          Threshold (미등록 시 기본값)
     */
    public Threshold getThreshold(String type, String domain, String id) {
        return ruleCacheService
                .getThreshold(type, domain, id)
                .orElse(new Threshold());
    }
}
