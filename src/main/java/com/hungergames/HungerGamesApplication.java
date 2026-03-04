package com.hungergames;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HungerGamesApplication {
    public static void main(String[] args) {
        SpringApplication.run(HungerGamesApplication.class, args);
    }
}
