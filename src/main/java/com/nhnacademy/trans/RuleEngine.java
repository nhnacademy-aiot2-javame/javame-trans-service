package com.nhnacademy.trans;

import com.nhnacademy.trans.domain.Threshold;

public interface RuleEngine {
    boolean evaluate(String data, Threshold threshold);
}