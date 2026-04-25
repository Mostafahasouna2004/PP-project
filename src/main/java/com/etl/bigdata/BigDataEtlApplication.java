package com.etl.bigdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BigDataEtlApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigDataEtlApplication.class, args);
    }
}
