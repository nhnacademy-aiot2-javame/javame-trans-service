package com.nhnacademy.trans;

import com.nhnacademy.trans.domain.SensorData;
import com.nhnacademy.trans.domain.Threshold;
import org.springframework.stereotype.Component;

public interface RuleEngine {
    boolean evaluate(String data, Threshold threshold);
}