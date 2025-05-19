package com.nhnacademy.trans.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/***
 * Redis 에서 사용할 임계값을 저장하는 클래스
 */
@Getter
@RequiredArgsConstructor
public class RuleCache {

    /***
     * 서버,센서의 식별 아이디 (서버 호스트, 센서 아이디)
     */
    private final String id;

    /***
     * CompanyDomain -Client 대표 식별자
     */
    private final String companyDomain;

    /***
     * 임계값 저장 Map
     */
    private final Map<String, Threshold> rules;

}