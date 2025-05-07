package com.nhnacademy.trans.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class RuleCache {
    private final String id;
    private final String companyDomain;
    private final Map<String, Threshold> rules;

}