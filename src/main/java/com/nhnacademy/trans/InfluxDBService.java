package com.nhnacademy.trans;

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

@Service
@RequiredArgsConstructor
public class InfluxDBService {
    private final InfluxDBClient influxDBClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void save(String topic, String payload) throws Exception {
        // 1. payload 파싱
        JsonNode root = objectMapper.readTree(payload);
        long time = root.get("time").asLong();
        JsonNode valueNode = root.get("value");

        // 2. topic 동적 파싱 → map으로 변환
        String[] tokens = topic.split("/");
        Map<String,String> map = new HashMap<>();
        // tokens: ["data","s","nhnacademy","b","gyeongnam_campus", ... ,"e","lora"]
        for(int i=1; i<tokens.length-1; i+=2){
            map.put(tokens[i], tokens[i+1]);
        }

        String companyDomain = map.get("s");
        String building      = map.get("b");
        String place         = map.get("p");
        String serverId      = map.get("d");
        String location      = map.get("n");
        String gatewayId     = map.get("g");      // g가 없으면 null
        String measurement   = map.get("e");      // 반드시 있어야 함

        // 3. Point 생성 (value가 primitive vs object 구분)
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
        if (valueNode.isObject()) {
            // lora 같은 복합 필드
            List<Point> points = new ArrayList<>();
            valueNode.fields().forEachRemaining(entry -> {
                Point p = Point.measurement(measurement)
                        .addTag("companyDomain", companyDomain)
                        .addTag("building", building)
                        .addTag("place", place)
                        .addTag("serverId", serverId)
                        .addTag("location", location);
                if (gatewayId!=null) p.addTag("gatewayId", gatewayId);

                // 숫자면 double, 아니면 text
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
            // humidity, temperature 처럼 단일 값
            double value = valueNode.asDouble();
            Point p = Point.measurement(measurement)
                    .addTag("companyDomain", companyDomain)
                    .addTag("building", building)
                    .addTag("place", place)
                    .addTag("serverId", serverId)
                    .addTag("location", location);
            if (gatewayId!=null) p.addTag("gatewayId", gatewayId);

            p.addField("value", value)
                    .time(Instant.ofEpochMilli(time), WritePrecision.MS);
            writeApi.writePoint(p);
        }
    }

}
