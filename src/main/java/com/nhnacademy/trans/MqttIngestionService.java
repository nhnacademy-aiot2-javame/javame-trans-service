package com.nhnacademy.trans;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.nhnacademy.trans.domain.SensorData;
import com.nhnacademy.trans.domain.Threshold;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class MqttIngestionService {

    @Value("${mqtt.port}")
    private int port;

    @Value("${mqtt.serverHost}")
    private String serverHost;

    private final RuleEngine ruleEngine;
    private final ThresholdCacheManager thresholdCacheManager;

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
                            .topicFilter("#")
                            .callback(this::handleMessage)
                            .send();
                });
    }

    private void handleMessage(Mqtt3Publish publish) {
        try {
            String topic = publish.getTopic().toString();
            String payload = StandardCharsets.UTF_8.decode(publish.getPayload().get()).toString();

            log.info("topic: {}, payload: {}", topic, payload);

            SensorData data = parse(payload);
            Threshold threshold = thresholdCacheManager.getThreshold(data.getSensorId());

            boolean isTriggered = ruleEngine.evaluate(data, threshold);

            if (isTriggered) {
                log.warn("임계값 초과: {}", data);
                // 알람 발송 or 처리
            }

            // InfluxDB 저장 로직
        } catch (Exception e) {
            log.error("MQTT 메시지 처리 중 오류", e);
        }
    }

    private SensorData parse(String payload) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(payload, SensorData.class);
    }
}
