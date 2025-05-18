package com.nhnacademy.trans.config;

import com.nhnacademy.trans.domain.Threshold;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
/**
 * Redis 설정 클래스.
 * <p>Spring Data Redis를 사용하기 위한 Lettuce 기반의
 * RedisConnectionFactory 및 RedisTemplate<String, Threshold> 빈을 등록한다.
 */
@Configuration
public class RedisConfig {

    /**
     * Redis 서버 호스트 주소
     */
    @Value("${spring.data.redis.host}")
    private String redisHost;

    /**
     * Redis 서버 포트
     */
    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Redis 사용자 이름 (ACL 모드 사용 시)
     */
    @Value("${spring.data.redis.username:}")
    private String redisUsername;

    /**
     * Redis 비밀번호
     */
    @Value("${spring.data.redis.password}")
    private String redisPassword;

    /**
     * Redis 데이터베이스 인덱스
     */
    @Value("${spring.data.redis.database}")
    private int redisDatabase;

    /**
     * RedisConnectionFactory 빈을 생성한다.
     * <p>Stand-alone 모드로 설정된 Redis 서버에 연결하기 위한 LettuceConnectionFactory를 반환한다.
     *
     * @return LettuceConnectionFactory 인스턴스
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration cfg =
                new RedisStandaloneConfiguration(redisHost, redisPort);
        cfg.setDatabase(redisDatabase);
        if (!redisUsername.isBlank()) {
            cfg.setUsername(redisUsername);
        }
        cfg.setPassword(RedisPassword.of(redisPassword));

        return new LettuceConnectionFactory(cfg);
    }

    /**
     * RedisTemplate<String, Threshold> 빈을 생성한다.
     * <p>키는 String, 값은 JSON 직렬화를 사용하는 Threshold 객체로 설정한다.
     *
     * @param cf RedisConnectionFactory
     * @return RedisTemplate<String, Threshold> 인스턴스
     */
    @Bean
    public RedisTemplate<String, Threshold> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Threshold> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setHashKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        tpl.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return tpl;
    }
}
