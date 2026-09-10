package com.codecrafthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal — ponto de entrada da aplicação Spring Boot.
 *
 * <p>Execute com: {@code mvn spring-boot:run} ou via Docker.</p>
 * <p>API disponível em: http://localhost:8080</p>
 */
@SpringBootApplication
public class CodeCraftHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeCraftHubApplication.class, args);
        System.out.println("---------------------------------------------");
        System.out.println("CodeCraftHub API is starting...");
        System.out.println("Data will be stored in: courses.json");
        System.out.println("API will be available at: http://localhost:8080");
        System.out.println("---------------------------------------------");
    }
}
