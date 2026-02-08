package com.ametsa.smartbachat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PdfParserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PdfParserServiceApplication.class, args);
    }
}
