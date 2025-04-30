package com.nhnacademy.trans.domain;

import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;
import java.util.Map;

@RedisHash("rule")
@Getter
public class RuleCache {
    private String id;  // sensorId or serverIp
    private Map<String, Threshold> rules; // 예: temperature → min/max

}
