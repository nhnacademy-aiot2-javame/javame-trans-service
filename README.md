![image](https://github.com/user-attachments/assets/6e4eb6fc-b44c-4a9e-ab02-2d707dfba425)
# trans-service

`trans-service`는 IoT 센서 데이터를 MQTT 또는 Kafka를 통해 수신하여 InfluxDB에 저장하는 Spring Boot 기반의 마이크로서비스입니다. 각 센서 데이터는 사전에 등록된 서버 및 센서 정보에 기반하여 처리되며, 룰 엔진에서 임계값 기반 warnify-service 와 연동되어 알림 발생 기능을 수행합니다.

---

## 💡 주요 기능

- MQTT / Kafka를 통해 IoT 센서 데이터 수신
- 센서 데이터 정제 및 파싱
- threshold 기반 Rule Engine과 연계 가능
- InfluxDB에 시계열 데이터로 저장
- 센서 및 서버 식별 정보 기반 데이터 처리


---

## 📦 기술 스택

- Java / Spring Boot
- MQTT (e.g. Hive Mq)
- InfluxDB + Spring InfluxDB Client
- Redis (센서 threshold 캐싱 및 룰 엔진 연계)
- Docker / Docker-Compose

---

## 📡 센서 데이터 흐름

### 1. MQTT 수신 구조

토픽 예시:
data/s/{companyDomain}/b/{building}/p/{place}/d/{serverId}/n/{location}/g/{gatewayId}/e/lora



Payload 예시:
```json
{
  "timestamp": "2025-06-10T12:00:00Z",
  "sensors": [
    {
      "sensorId": "temperature-001",
      "value": 28.4,
      "unit": "C"
    },
    {
      "sensorId": "battery-001",
      "value": 95.1,
      "unit": "%"
    }
  ]

}
```

# 🗄️ InfluxDB 저장 구조
Measurement: 각 센서 수집 값 이름

Tags:

companyDomain, serverId, sensorId, location, gatewayId

Fields:

value, unit, timestamp


Redis 기반 Rule Cache 연동

Threshold 설정 시 이벤트 발생 처리

Alert 시스템 연동 (email, Slack 등)




