package com.flowforge.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class EventProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventProcessorApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public java.time.Clock clock() {
        return java.time.Clock.systemUTC();
    }
}
