package com.nhnacademy.trans.adaptor;

import com.nhnacademy.trans.domain.RuleCache;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient(name = "rule-api")
public interface RuleAdaptor {

    @GetMapping("/sensor-data")
    ResponseEntity<List<RuleCache>> findAllSensorData();
    @GetMapping("/server-data")
    ResponseEntity<List<RuleCache>> findAllServerData();
}
