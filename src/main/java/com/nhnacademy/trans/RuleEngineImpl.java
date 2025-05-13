package com.nhnacademy.trans;

import com.nhnacademy.trans.domain.Threshold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RuleEngineImpl implements RuleEngine {
    private static final Logger log = LoggerFactory.getLogger(RuleEngineImpl.class);

    @Override
    public boolean evaluate(String data, Threshold threshold) {
        log.warn("RuleEngineImpl: evaluate");
        return false;
    }
}
