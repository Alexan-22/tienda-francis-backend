package com.tiendafrancis.comercio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ComercioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComercioApplication.class, args);
    }
}