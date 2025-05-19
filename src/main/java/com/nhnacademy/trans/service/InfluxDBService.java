package com.nhnacademy.trans.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

import com.influxdb.client.write.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code InfluxDBService}는 MQTT로 전달된 토픽과 JSON 페이로드를
 * InfluxDB에 비동기 배치 쓰기 방식으로 저장하는 서비스 클래스입니다.
 * <p>
 * 토픽은 'data' 또는 'server_data'로 시작하며, 이어서 태그 키(key)/값(value) 쌍이 구성됩니다.
 * 페이로드는 JSON으로, 'time' 필드와 'value' 필드를 포함해야 합니다.
 * 'value'는 단일 숫자이거나, 복합 객체(JSON)일 수 있습니다.
 *
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class InfluxDBService {

    /***
     * url.
     */
    @Value("${influx.url}")
    private String url;

    /***
     * url.
     */
    @Value("${influx.token}")
    private String token;

    /***
     * url.
     */
    @Value("${influx.org}")
    private String org;

    /***
     * url.
     */
    @Value("${influx.bucket}")
    private String bucket;

    /***
     * url.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /***
     * url.
     */
    private InfluxDBClient influxDBClient;

    /***
     * writeAPI.
     */
    private WriteApi writeApi;

    /**
     * Bean 초기화 후 InfluxDB 클라이언트를 설정 및 연결 검증.
     */
    @PostConstruct
    public void init() {
        this.influxDBClient = InfluxDBClientFactory.create(url,token.toCharArray(),org,bucket);

        WriteOptions options = WriteOptions.builder()
                .batchSize(500)
                .flushInterval(2000)
                .retryInterval(3)
                .retryInterval(5000)
                .build();
        this.writeApi = influxDBClient.makeWriteApi(options);
    }

    @PreDestroy
    public void shutdown() {
        if (writeApi != null) {
            writeApi.close();
        }
        if (influxDBClient != null) {
            influxDBClient.close();
        }
    }


    /**
     * MQTT 토픽과 JSON 페이로드를 받아 InfluxDB에 저장합니다.
     * <ul>
     *   <li>토픽을 파싱하여 태그로 변환</li>
     *   <li>payload의 'time'과 'value'를 추출</li>
     *   <li>단일 값인 경우 하나의 필드를, 객체인 경우 각각의 필드를 Point로 생성</li>
     *   <li>BatchPoints로 묶어 한 번에 전송</li>
     * </ul>
     *
     * @param topic   MQTT 토픽 (예: server_data/s/nhnacademy/…/e/measurement)
     * @param payload JSON 문자열 {"time":<ms>,"value":<number|object>}
     */
    public void save(String topic, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            long time = root.path("time").asLong();
            JsonNode valueNode = root.path("value");

            // 토픽 -> 태그 맵 변환
            String[] tokens = topic.split("/");
            Map<String, String> tags = new HashMap<>();
            for (int i = 1; i < tokens.length - 1; i += 2) {
                tags.put(tokens[i], tokens[i + 1]);
            }
            String measurement = tags.get("e");

            // 단일 값 또는 복합 객체 처리
            if (valueNode.isObject()) {
                valueNode.fields().forEachRemaining(entry -> {
                    Point p = buildPoint(measurement, tags, entry.getKey(), entry.getValue(), time);
                    writeApi.writePoint(p);
                });
            } else {
                Point p = buildPoint(measurement, tags, "value", valueNode, time);
                writeApi.writePoint(p);
            }
        } catch (Exception e) {
        }
    }

    /**
     * 단일 Point를 생성하는 헬퍼 메서드입니다.
     *
     * @param measurement 측정값 이름
     * @param tags        태그 키-값 맵
     * @param fieldKey    필드 이름
     * @param fieldValue  필드 값(JsonNode)
     * @param timestamp   타임스탬프(ms)
     * @return Point 인스턴스
     */
    private Point buildPoint(
            String measurement,
            Map<String, String> tags,
            String fieldKey,
            JsonNode fieldValue,
            long timestamp
    ) {
        return Point.measurement(measurement)
                .addTags(tags)
                .addField(fieldKey, fieldValue.asDouble())
                .time(Instant.ofEpochMilli(timestamp), WritePrecision.MS);
    }
}
