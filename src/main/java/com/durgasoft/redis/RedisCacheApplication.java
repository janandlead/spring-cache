package com.durgasoft.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RedisCacheApplication {
    /** Starts the Spring Boot application and initializes the REST, JPA, and Redis components. */
    public static void main(String[] args) {
        SpringApplication.run(RedisCacheApplication.class, args);
    }
}
