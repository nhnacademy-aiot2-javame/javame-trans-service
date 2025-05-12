package com.nhnacademy.trans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class MqttIngestionService {

    @Value("${mqtt.port}")
    private int port;

    @Value("${mqtt.serverHost}")
    private String serverHost;

    private final RuleEngine ruleEngine;
    private final InfluxDBService influxDBService;
    private final RuleCacheService ruleCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Mqtt3AsyncClient client;

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

    private void handleMessage(Mqtt3Publish publish) {
        try {
            String topic = publish.getTopic().toString();
            String payload = StandardCharsets.UTF_8.decode(publish.getPayload().get()).toString();

            log.warn("topic: {}, payload: {}", topic, payload);

            // 데이터 타입 파싱
            String type = extractDataType(topic);
            String companyDomain = extractCompanyDomain(topic);


            // 3. value가 단일 숫자인지 확인
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
                // 알림 전송(Warnify service 호출)
            }
            // Influx 저장은 모든 경우에 처리
            influxDBService.save(topic, payload);


        } catch (Exception e) {
            log.error("MQTT 메시지 처리 중 오류", e);
        }
    }

    private String extractSensorId(String topic) {
        // /g/ 다음으로 오는 문자열 '/' 전 까지 리턴
        return topic.contains("/d/") ? topic.split("/d/")[1].split("/")[0] : "UNKNOWN";
    }


    private String extractDataType(String topic) {
        return topic.substring(topic.lastIndexOf('/') + 1);
    }

    // companyDomain 추출 헬퍼 메서드 추가
    private String extractCompanyDomain(String topic) {
        // topic 예시: "data/s/myDomain/b/../..."
        String[] tokens = topic.split("/");
        return tokens.length > 2 ? tokens[2] : "UNKNOWN";
    }


}
