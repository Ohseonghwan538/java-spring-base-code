package com.map.egis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.map.egis.mapper")
public class JavaSpringBaseCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaSpringBaseCodeApplication.class, args);
    }

}