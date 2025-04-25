package com.nhnacademy.trans.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Threshold {
    private String sensorId;
    private String type;
    private double min;
    private double max;
}

