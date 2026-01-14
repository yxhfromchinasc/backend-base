package com.lvxin.base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LvxinBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(LvxinBaseApplication.class, args);
    }

}
