package com.nhnacademy.trans.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.nhnacademy.trans.RuleEngine;
import com.nhnacademy.trans.config.RuleCacheService;
import com.nhnacademy.trans.domain.Threshold;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
/**
 * MQTT로 수신된 센서 데이터를 처리하고 InfluxDB에 저장하거나 임계값 초과 시 알림을 수행하는 서비스.
 * <p>
 * - MQTT 브로커에 연결하여 지정된 토픽을 구독합니다.
 * - 수신된 메시지를 파싱하여 룰 엔진을 통해 임계값을 평가합니다.
 * - 임계값 초과 시 로그 또는 알림 서비스를 호출합니다.
 * - 모든 메시지를 InfluxDB에 기록합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MqttIngestionService {

    /** MQTT 브로커 포트 */
    @Value("${mqtt.port}")
    private int port;

    /** MQTT 서버 호스트 */
    @Value("${mqtt.serverHost}")
    private String serverHost;

    private final RuleEngine ruleEngine;
    private final InfluxDBService influxDBService;
    private final RuleCacheService ruleCacheService;

    /** JSON 파싱을 위한 ObjectMapper */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 비동기 MQTT 클라이언트 */
    private Mqtt3AsyncClient client;

    /**
     * Bean 초기화 후 실행되며, MQTT 브로커에 연결하고 토픽을 구독한다.
     */
    @PostConstruct
    public void init() {
        this.client = MqttClient.builder()
                .useMqttVersion3()
                .identifier("mqtt-ingestion-service")
                .serverHost(serverHost)
                .serverPort(port)
                .buildAsync();

        client.connectWith()
                .cleanSession(true)
                .send()
                .whenComplete((connAck, throwable) -> {
                    if (throwable != null) {
                        log.error("MQTT 연결 실패", throwable);
                        return;
                    }

                    client.subscribeWith()
                            .topicFilter("+/s/+/b/+/p/server_room/#")
                            .callback(this::handleMessage)
                            .send();
                });
    }

    /**
     * MQTT 메시지를 수신했을 때 호출되는 콜백 메서드.
     * <ol>
     *   <li>토픽과 페이로드를 문자열로 변환합니다.</li>
     *   <li>페이로드가 단일 값일 경우 JSON 형태로 변환합니다.</li>
     *   <li>RuleCacheService를 통해 임계값을 조회하고 RuleEngine으로 평가합니다.</li>
     *   <li>임계값 초과 시 알림 로그를 출력합니다.</li>
     *   <li>InfluxDBService를 통해 데이터를 저장합니다.</li>
     * </ol>
     *
     * @param publish 수신된 MQTT Publish 메시지
     */
    private void handleMessage(Mqtt3Publish publish) {
        try {
            String topic = publish.getTopic().toString();
            String payload = StandardCharsets.UTF_8.decode(publish.getPayload().get()).toString();

            log.warn("topic: {}, payload: {}", topic, payload);

            // 데이터 타입 및 도메인 파싱
            String type = extractDataType(topic);
            String companyDomain = extractCompanyDomain(topic);

            // 단일 숫자 페이로드인 경우 JSON 형태로 변환
            if (payload.split(",").length == 1) {
                Map<String, Object> payloadMap = new HashMap<>();
                payloadMap.put("time", System.currentTimeMillis());
                payloadMap.put("value", payload);
                payload = objectMapper.writeValueAsString(payloadMap);
            }

            String sensorId = extractSensorId(topic);
            log.warn("sensorId: {}", sensorId);

            Optional<Threshold> threshold = ruleCacheService.getThreshold(type, companyDomain, sensorId);
            String sensorValue = payload.split(":")[2];

            boolean isTriggered = ruleEngine.evaluate(sensorValue, threshold.orElse(null));

            if (isTriggered) {
                log.warn("임계값 초과 알림: {}, value={}", type, sensorValue);
                // 알림 전송 로직 추가 가능
            }
            // InfluxDB에 데이터 저장
            influxDBService.save(topic, payload);

        } catch (Exception e) {
            log.error("MQTT 메시지 처리 중 오류", e);
        }
    }

    /**
     * 토픽에서 센서 ID를 추출한다.
     *
     * @param topic MQTT 토픽
     * @return 센서 ID (토픽에 "/d/"가 없으면 "UNKNOWN")
     */
    private String extractSensorId(String topic) {
        return topic.contains("/d/") ? topic.split("/d/")[1].split("/")[0] : "UNKNOWN";
    }

    /**
     * 토픽에서 데이터 타입(마지막 토큰)을 추출한다.
     *
     * @param topic MQTT 토픽
     * @return 데이터 타입 문자열
     */
    private String extractDataType(String topic) {
        return topic.substring(topic.lastIndexOf('/') + 1);
    }

    /**
     * 토픽에서 companyDomain 값을 추출한다.
     *
     * @param topic MQTT 토픽
     * @return companyDomain (토큰이 부족하면 "UNKNOWN")
     */
    private String extractCompanyDomain(String topic) {
        String[] tokens = topic.split("/");
        return tokens.length > 2 ? tokens[2] : "UNKNOWN";
    }

}
