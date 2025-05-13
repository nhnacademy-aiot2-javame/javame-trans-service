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

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.username:}")
    private String redisUsername;          // ACL 모드용 (없으면 빈 문자열)

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.data.redis.database}")
    private int redisDatabase;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 1) standalone 설정: 호스트/포트, DB 인덱스, (선택) 사용자·비밀번호
        RedisStandaloneConfiguration cfg =
                new RedisStandaloneConfiguration(redisHost, redisPort);
        cfg.setDatabase(redisDatabase);
        if (!redisUsername.isBlank()) {
            cfg.setUsername(redisUsername);
        }
        cfg.setPassword(RedisPassword.of(redisPassword));

        // 2) LettuceConnectionFactory 생성
        return new LettuceConnectionFactory(cfg);
    }

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
