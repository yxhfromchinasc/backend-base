package com.lvxin.base.config;

import com.lvxin.base.util.ThreadPoolManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;

/**
 * 异步线程池配置
 * 将ThreadPoolManager管理的线程池注册为Spring Bean，供@Async注解使用
 */
@Configuration
public class AsyncBeanConfig {

    @Bean("defaultExecutor")
    public Executor defaultExecutor(ThreadPoolManager threadPoolManager) {
        // 将ThreadPoolManager中的默认线程池注册为Spring Bean
        // 这样 @Async("defaultExecutor") 就能找到它了
        return threadPoolManager.defaultExecutor().getExecutor();
    }
}
