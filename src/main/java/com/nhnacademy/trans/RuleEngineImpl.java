package com.nhnacademy.trans;

import com.nhnacademy.trans.domain.SensorData;
import com.nhnacademy.trans.domain.Threshold;
import org.springframework.stereotype.Component;

@Component
public class RuleEngineImpl implements RuleEngine {
    @Override
    public boolean evaluate(SensorData data, Threshold threshold) {
        return true;
    }
}
