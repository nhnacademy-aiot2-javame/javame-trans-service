classDiagram
%% Domain / DTO
class SensorDataDTO {
+String sensorId
+String metric
+double value
+Instant timestamp
}
class Threshold {
+String sensorId
+String metric
+double min
+double max
+boolean userDefined
}
class Alert {
+UUID id
+String sensorId
+String metric
+double value
+Instant timestamp
+String message
}

%% Services
class MqttIngestionService {
  +void onMessage(SensorDataDTO data)
}
class RuleEngineService {
  +boolean evaluate(SensorDataDTO data)
}
class ThresholdCacheManager {
  +Threshold getThreshold(String sensorId)
  +void updateThreshold(Threshold t)
}
class InfluxWriteService {
  +void write(SensorDataDTO data)
}
class AlertService {
  +void sendAlert(Alert alert)
}

%% Controllers & Service Layer
class ThresholdService {
  +Threshold getThreshold(String sensorId)
  +void saveThreshold(Threshold t)
}
class ThresholdController {
  +Threshold getThreshold(String sensorId)
  +void updateThreshold(String sensorId, Threshold t)
}

%% 관계 정의
MqttIngestionService --> RuleEngineService           : uses
RuleEngineService --> ThresholdCacheManager          : looks up
ThresholdCacheManager --> ThresholdService           : loads/saves
RuleEngineService --> InfluxWriteService             : onNormal
RuleEngineService --> AlertService                   : onAlert
ThresholdController --> ThresholdService             : delegates

%% DTO ↔ Domain
MqttIngestionService --> SensorDataDTO               : receives
RuleEngineService --> SensorDataDTO                  : processes
RuleEngineService --> Alert                          : creates
