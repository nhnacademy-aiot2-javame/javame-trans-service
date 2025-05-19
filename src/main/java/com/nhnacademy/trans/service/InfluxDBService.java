package com.nhnacademy.trans.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Pong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
@Slf4j
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
    @Value("${influx.token:}")
    private String token;

    /***
     * url.
     */
    @Value("${influx.org}")
    private String database;

    /***
     * url.
     */
    @Value("${influx.retention:autogen}")
    private String retention;

    /***
     * url.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /***
     * url.
     */
    private InfluxDB influxDB;

    /**
     * Bean 초기화 후 InfluxDB 클라이언트를 설정 및 연결 검증.
     */
    @PostConstruct
    public void init() {
        // 짧은 타임아웃을 가진 OkHttpClient.Builder 생성
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS);

        // 클라이언트 생성: Builder를 넘겨야 합니다
        this.influxDB = InfluxDBFactory.connect(url, "javame", token, clientBuilder);
        influxDB.setDatabase(database);
        influxDB.setRetentionPolicy(retention);
        // 비동기 배치 쓰기 활성화
        influxDB.enableBatch();
        log.info("InfluxDB client initialized (db={}, retention={})", database, retention);
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

            // BatchPoints 생성
            BatchPoints batch = BatchPoints
                    .database(database)
                    .retentionPolicy(retention)
                    .build();

            // value가 객체인지 단일 값인지 분기 처리
            if (valueNode.isObject()) {
                valueNode.fields().forEachRemaining(entry -> {
                    Point point = createPoint(measurement, tags, entry.getKey(), entry.getValue(), time);
                    batch.point(point);
                });
            } else {
                Point point = createPoint(measurement, tags, "value", valueNode, time);
                batch.point(point);
            }

            // InfluxDB에 배치 쓰기
            influxDB.write(batch);

        } catch (Exception e) {
            log.error("Failed to write to InfluxDB for topic {}", topic, e);
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
    private Point createPoint(
            String measurement,
            Map<String, String> tags,
            String fieldKey,
            JsonNode fieldValue,
            long timestamp
    ) {
        Point.Builder builder = Point.measurement(measurement)
                .time(timestamp, TimeUnit.MILLISECONDS);

        // 태그 추가
        tags.forEach(builder::tag);
        // 필드 추가
        if (fieldValue.isNumber()) {
            builder.addField(fieldKey, fieldValue.asDouble());
        } else {
            builder.addField(fieldKey, fieldValue.asText());
        }

        return builder.build();
    }
}
