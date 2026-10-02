package com.nikyfarm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NikyfarmApplication {

    public static void main(String[] args) {
        SpringApplication.run(NikyfarmApplication.class, args);
    }
}
