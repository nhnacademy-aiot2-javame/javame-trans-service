package com.nhnacademy.trans.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
    private String sensorId;
    private String type;
    private double value;
    private long timestamp;
}
