package com.nhnacademy.trans.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/***
 * 임계값을 관리하는 클래스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Threshold {

    /***
     * 최솟값
     */
    private double min;

    /***
     * 최댓값
     */
    private double max;
}

