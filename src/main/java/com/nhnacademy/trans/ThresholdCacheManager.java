package com.nhnacademy.trans;

import com.nhnacademy.trans.domain.Threshold;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ThresholdCacheManager {

    private final Map<String, Threshold> cache = new ConcurrentHashMap<>();

    public Threshold getThreshold(String sensorId) {
        return cache.getOrDefault(sensorId, new Threshold(sensorId, 0, 100)); // 기본값
    }

    public void updateThreshold(String sensorId, Threshold threshold) {
        cache.put(sensorId, threshold);
    }
}
