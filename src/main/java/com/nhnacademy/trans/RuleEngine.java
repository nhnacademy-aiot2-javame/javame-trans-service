package com.nhnacademy.trans;

import com.nhnacademy.trans.domain.Threshold;

/**
 * Rule-engine 인터페이스.
 */
public interface RuleEngine {
    boolean evaluate(String data, Threshold threshold);
}