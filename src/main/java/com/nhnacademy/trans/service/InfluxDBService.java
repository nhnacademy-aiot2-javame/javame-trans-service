package com.nhnacademy.trans.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.write.Point;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InfluxDB에 센서 데이터를 저장하는 서비스.
 * <p>MQTT 또는 기타 소스로부터 전달받은 토픽과 페이로드를 파싱하여
 * Point 객체로 변환한 뒤 InfluxDB에 기록한다.
 */
@Service
@RequiredArgsConstructor
public class InfluxDBService {

    private static final Logger log = LoggerFactory.getLogger(InfluxDBService.class);
    /**
     * InfluxDB 클라이언트
     */
    private final InfluxDBClient influxDBClient;

    /***
     * writeApi 비동기 방식.
     */
    private WriteApi writeApi;

    /**
     * JSON 파싱을 위한 ObjectMapper.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Bean 초기화 완료 후 비동기 WriteApi 설정.
     */
    @PostConstruct
    public void initWriteApi() {
        this.writeApi = influxDBClient.makeWriteApi(
                WriteOptions.builder()
                        .batchSize(500)      // 500개 포인트마다 전송
                        .flushInterval(2000) // 2초마다 플러시
                        .retryInterval(3)    // 실패 시 3회 재시도
                        .retryInterval(5000) // 재시도 간격 5초
                        .build()
        );
    }

    /**
     * 주어진 토픽과 JSON 페이로드를 파싱하여 InfluxDB에 비동기 저장한다.
     * @param topic   MQTT 토픽 문자열
     * @param payload JSON 형식의 문자열
     * @throws Exception 파싱 오류 발생 시
     */
    public void save(String topic, String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        long time = root.get("time").asLong();
        JsonNode valueNode = root.get("value");

        // 토픽 파싱
        String[] tokens = topic.split("/");
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i < tokens.length - 1; i += 2) {
            map.put(tokens[i], tokens[i + 1]);
        }

        String companyDomain = map.get("s");
        String building      = map.get("b");
        String place         = map.get("p");
        String deviceId      = map.get("d");
        String location      = map.get("n");
        String gatewayId     = map.get("g");
        String measurement   = map.get("e");
        String origin        = tokens[0].equals("data") ? "sensor_data" : "server_data";

        if (valueNode.isObject()) {
            List<Point> points = new ArrayList<>();
            valueNode.fields().forEachRemaining(entry -> {
                Point p = Point.measurement(measurement)
                        .addTag("companyDomain", companyDomain)
                        .addTag("building", building)
                        .addTag("place", place)
                        .addTag("device_id", deviceId)
                        .addTag("location", location)
                        .addTag("origin", origin);
                if (gatewayId != null) p.addTag("gatewayId", gatewayId);
                if (entry.getValue().isNumber()) {
                    p.addField(entry.getKey(), entry.getValue().asDouble());
                } else {
                    p.addField(entry.getKey(), entry.getValue().asText());
                }
                p.time(Instant.ofEpochMilli(time), com.influxdb.client.domain.WritePrecision.MS);
                points.add(p);
            });
            writeApi.writePoints(points);
            log.debug("저장 완료");
        } else {
            double value = valueNode.asDouble();
            Point p = Point.measurement(measurement)
                    .addTag("companyDomain", companyDomain)
                    .addTag("building", building)
                    .addTag("place", place)
                    .addTag("device_id", deviceId)
                    .addTag("location", location)
                    .addTag("origin", origin);
            if (gatewayId != null) p.addTag("gatewayId", gatewayId);
            p.addField("value", value)
                    .time(Instant.ofEpochMilli(time), com.influxdb.client.domain.WritePrecision.MS);
            writeApi.writePoint(p);
            log.debug("저장 완료");
        }
    }

}
