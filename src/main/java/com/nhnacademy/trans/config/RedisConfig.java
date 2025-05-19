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
 * Redis 연결 및 RedisTemplate 설정을 제공하는 Configuration 클래스.
 * <p>Spring Data Redis를 사용하기 위한
 * <p>RedisConnectionFactory와<p>
 * RedisTemplate&lt;String, Threshold&gt; 빈을 정의한다.
 */
@Configuration
public class RedisConfig {

    /**
     * Redis 서버 호스트 주소.
     * <p>application.properties 또는 application.yml의
     * <code>spring.data.redis.host</code> 값을 주입받는다.
     */
    @Value("${spring.data.redis.host}")
    private String redisHost;

    /**
     * Redis 서버 포트 번호.
     * <p>application.properties 또는 application.yml의
     * <code>spring.data.redis.port</code> 값을 주입받는다.
     */
    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Redis ACL 모드용 사용자명.
     * <p>application.properties 또는 application.yml의
     * <code>spring.data.redis.username</code> 값을 주입받는다.
     * 기본값이 빈 문자열("")일 수 있다.
     */
    @Value("${spring.data.redis.username:}")
    private String redisUsername;

    /**
     * Redis 비밀번호.
     * <p>application.properties 또는 application.yml의
     * <code>spring.data.redis.password</code> 값을 주입받는다.
     */
    @Value("${spring.data.redis.password}")
    private String redisPassword;

    /**
     * 사용할 Redis 데이터베이스 인덱스.
     * <p>application.properties 또는 application.yml의
     * <code>spring.data.redis.database</code> 값을 주입받는다.
     */
    @Value("${spring.data.redis.database}")
    private int redisDatabase;


    /**
     * RedisConnectionFactory 빈을 생성한다.
     * <p>spring.data.redis.host, port, database, (선택)username, password 프로퍼티를 기반으로
     * {@link RedisStandaloneConfiguration}을 구성하고, 이를 이용한
     * {@link LettuceConnectionFactory}를 반환한다.
     *
     * @return Lettuce 기반의 RedisConnectionFactory
     */
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

        // 2) LettuceConnectionFactory 생성 & 초기화
        LettuceConnectionFactory factory = new LettuceConnectionFactory(cfg);
        factory.afterPropertiesSet();   // ← 이 한 줄 추가!
        return factory;
    }

    /**
     * RedisTemplate&lt;String, Threshold&gt; 빈을 생성한다.
     * <p>키와 해시 키에는 {@link StringRedisSerializer}를 사용하고,
     * 값과 해시 값에는 {@link GenericJackson2JsonRedisSerializer}를 사용하도록 설정한다.
     *
     * @param cf RedisConnectionFactory 빈
     * @return 제네릭 Jackson2 JSON 직렬화가 적용된 RedisTemplate&lt;String, Threshold&gt;
     */
    @Bean
    public RedisTemplate<String, Threshold> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Threshold> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setHashKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        tpl.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        tpl.afterPropertiesSet();
        return tpl;
    }
}
