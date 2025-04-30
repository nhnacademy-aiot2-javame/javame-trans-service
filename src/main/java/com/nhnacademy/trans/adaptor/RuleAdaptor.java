package com.nhnacademy.trans.adaptor;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "rule-api")
public interface RuleAdaptor {

}
