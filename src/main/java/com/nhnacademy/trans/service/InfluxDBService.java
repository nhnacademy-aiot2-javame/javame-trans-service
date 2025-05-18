package com.nhnacademy.trans.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.RequiredArgsConstructor;
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

    /**
     * InfluxDB 클라이언트
     */
    private final InfluxDBClient influxDBClient;

    /**
     * JSON 파싱을 위한 ObjectMapper
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 주어진 토픽과 JSON페이로드를 파싱하여 InfluxDB에 저장한다.
     * <ol>
     *   <li>payload를 JSON 트리로 읽어 시간(time)과 값(value)을 추출한다.</li>
     *   <li>토픽 문자열을 슬래시로 분리하여 키-값 맵(String→String)으로 변환한다.</li>
     *   <li>추출된 정보(companyDomain, building, place, deviceId, location, gatewayId, measurement, origin)를 태그로 설정한다.</li>
     *   <li>value가 객체일 경우 각 필드별로 Point를 생성하여 writePoints()로 배치 저장한다.</li>
     *   <li>value가 단일 숫자일 경우 하나의 Point를 생성하여 writePoint()로 저장한다.</li>
     * </ol>
     *
     * @param topic   MQTT 토픽 문자열 (예: data/s/{companyDomain}/b/{building}/...)
     * @param payload JSON 형식의 문자열 (예: {"time":1234567890, "value":{...}})
     * @throws Exception 페이로드 파싱 또는 InfluxDB 저장 중 오류 발생 시
     */
    public void save(String topic, String payload) throws Exception {
        // 1. payload JSON 파싱
        JsonNode root = objectMapper.readTree(payload);
        long time = root.get("time").asLong();
        JsonNode valueNode = root.get("value");

        // 2. topic 동적 파싱 → 키-값 맵 변환
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
        String gatewayId     = map.get("g");      // g가 없으면 null
        String measurement   = map.get("e");      // 반드시 있어야 함
        String origin        = tokens[0].equals("data") ? "sensor_data" : "server_data";

        // 3. Point 생성 및 쓰기
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
        if (valueNode.isObject()) {
            // 복합 필드 처리 (예: lora)
            List<Point> points = new ArrayList<>();
            valueNode.fields().forEachRemaining(entry -> {
                Point p = Point.measurement(measurement)
                        .addTag("companyDomain", companyDomain)
                        .addTag("building", building)
                        .addTag("place", place)
                        .addTag("device_id", deviceId)
                        .addTag("location", location)
                        .addTag("origin", origin);
                if (gatewayId != null) {
                    p.addTag("gatewayId", gatewayId);
                }
                if (entry.getValue().isNumber()) {
                    p.addField(entry.getKey(), entry.getValue().asDouble());
                } else {
                    p.addField(entry.getKey(), entry.getValue().asText());
                }
                p.time(Instant.ofEpochMilli(time), WritePrecision.MS);
                points.add(p);
            });
            writeApi.writePoints(points);
        } else {
            // 단일 값 처리 (예: temperature, humidity)
            double value = valueNode.asDouble();
            Point p = Point.measurement(measurement)
                    .addTag("companyDomain", companyDomain)
                    .addTag("building", building)
                    .addTag("place", place)
                    .addTag("device_id", deviceId)
                    .addTag("location", location)
                    .addTag("origin", origin);
            if (gatewayId != null) {
                p.addTag("gatewayId", gatewayId);
            }
            p.addField("value", value)
                    .time(Instant.ofEpochMilli(time), WritePrecision.MS);
            writeApi.writePoint(p);
        }
    }
}