package com.nhnacademy.trans;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
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
    private final InfluxDBService influxDBService;
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
                            .topicFilter("data/s/+/b/+/p/server_room/#")
                            .callback(this::handleMessage)
                            .send();
                });
    }

    private void handleMessage(Mqtt3Publish publish) {
        try {
            String topic = publish.getTopic().toString();
            String payload = StandardCharsets.UTF_8.decode(publish.getPayload().get()).toString();

            log.info("topic: {}, payload: {}", topic, payload);

            // 센서 타입 파싱
            String sensorType = extractSensorType(topic);

            // 파싱된 센서 데이터


            // 3. value가 단일 숫자인지 확인
            if (payload.split(",").length < 3) {
                String sensorId = extractSensorId(topic);
                Threshold threshold = thresholdCacheManager.getThreshold(sensorId, sensorType);
                String sensorValue = payload.split(":")[3];
                boolean isTriggered = ruleEngine.evaluate( sensorValue, threshold);

                if (isTriggered) {
                    log.warn("임계값 초과 알림: {}, value={}", sensorType, sensorValue);
                    // 알림 전송(Warnify service 호출)
                }
            }

            // Influx 저장은 모든 경우에 처리
            influxDBService.save(topic,payload);

        } catch (Exception e) {
            log.error("MQTT 메시지 처리 중 오류", e);
        }
    }

    private String extractSensorId(String topic) {
        // /g/ 다음으로 오는 문자열 '/' 전 까지 리턴
        return topic.contains("/g/") ? topic.split("/g/")[1].split("/")[0] : "UNKNOWN";
    }



    private String extractSensorType(String topic) {
        return topic.substring(topic.lastIndexOf('/') + 1);
    }



}
