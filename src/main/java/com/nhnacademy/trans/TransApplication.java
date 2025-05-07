package com.nhnacademy.trans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TransApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransApplication.class, args);
	}

}
