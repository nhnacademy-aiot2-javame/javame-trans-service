package com.nhnacademy.trans.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB 클라이언트 생성을 위한 Spring Configuration 클래스.
 * <p>
 * application.properties 또는 환경 변수로부터 InfluxDB 연결 정보를 주입 받아
 * {@link InfluxDBClient} Bean을 생성 및 등록한다.
 *
 * @author 거니장
 * @since 1.0
 */
@Configuration
public class InfluxDBConfig {

    /**
     * InfluxDB 서버 URL.
     */
    @Value("${influx.url}")
    private String influxUrl;

    /**
     * InfluxDB 접근용 인증 토큰.
     */
    @Value("${influx.token}")
    private String token;

    /**
     * InfluxDB 조직(organization) 식별자.
     */
    @Value("${influx.org}")
    private String org;

    /**
     * InfluxDB 버킷(bucket) 이름.
     */
    @Value("${influx.bucket}")
    private String bucket;

    /**
     * {@link InfluxDBClient} Bean을 생성하여 Spring 컨텍스트에 등록한다.
     * <p>
     * 주입된 URL, 토큰, 조직, 버킷 정보를 사용해
     * {@link InfluxDBClientFactory#create(String, char[], String, String)}를 호출하여
     * InfluxDBClient 객체를 생성한다.
     *
     * @return 구성된 {@link InfluxDBClient} 인스턴스
     */
    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(
                influxUrl,
                token.toCharArray(),
                org,
                bucket
        );
    }
}
