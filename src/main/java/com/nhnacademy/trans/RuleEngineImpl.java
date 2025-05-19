package com.nhnacademy.trans;

import com.nhnacademy.trans.domain.Threshold;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * RuleEngine 인터페이스의 기본 구현체.
 * <p>
 * 센서로부터 전달된 데이터 값과 설정된 임계값 {@link Threshold}를 비교하여
 * 임계값 초과 여부를 판단한다.
 */
@Component
@Slf4j
public class RuleEngineImpl implements RuleEngine {

    /**
     * 주어진 데이터 값과 임계값을 비교하여 임계값 초과 여부를 반환한다.
     * <ul>
     *   <li>threshold가 {@code null}이거나 임계값이 설정되지 않은 경우 항상 {@code false}를 반환</li>
     *   <li>threshold가 존재하면 실제 비교 로직을 통해 결과를 반환 (현재 TODO)</li>
     * </ul>
     *
     * @param data      센서로부터 수신된 데이터 값 (문자열)
     * @param threshold 비교 기준이 되는 Threshold 객체
     * @return 데이터 값이 임계값을 초과하면 {@code true}, 그렇지 않으면 {@code false}
     */
    @Override
    public boolean evaluate(String data, Threshold threshold) {
        log.warn("RuleEngineImpl: evaluate - data={} , threshold={}", data, threshold);
        if (threshold == null) {
            // 임계값 정보가 없으면 평가하지 않음
            return false;
        }
        // TODO: 실제 threshold 비교 로직 구현
        return false;
    }
}
