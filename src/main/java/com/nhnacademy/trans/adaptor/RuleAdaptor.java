package com.nhnacademy.trans.adaptor;

import com.nhnacademy.trans.domain.RuleCache;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/***
 * javame-rule-api 연결 어댑터
 */
@FeignClient(name = "rule-api")
public interface RuleAdaptor {

    /***
     * 센서 데이터를 불러오는 어댑터
     * @return RuleCache 로 통합 처리된 상태로 전달
     */
    @GetMapping("/sensor-data")
    ResponseEntity<List<RuleCache>> findAllSensorData();

    /***
     * 서버 데이터를 불러오는 어댑터
     * @return RuleCache 로 통합 처리된 상태로 전달
     */
    @GetMapping("/server-data")
    ResponseEntity<List<RuleCache>> findAllServerData();
}
