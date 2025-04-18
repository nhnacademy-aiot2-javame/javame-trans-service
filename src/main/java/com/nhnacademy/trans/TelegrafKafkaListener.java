package com.nhnacademy.trans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TelegrafKafkaListener {

    private static final Logger log = LogManager.getLogger(TelegrafKafkaListener.class);

    @KafkaListener(topics = "telegraf.metrics", groupId = "influx-writer")
    public void listen(String message) {
      log.info(message);


    }
}
