package com.lvxin.base;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableRetry
@SpringBootApplication
@MapperScan("com.lvxin.base.repository")
public class LvxinBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(LvxinBaseApplication.class, args);
    }

}
