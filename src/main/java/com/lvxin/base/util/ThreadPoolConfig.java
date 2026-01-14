package com.lvxin.base.util;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 自定义线程池配置类
 * 允许灵活配置线程池参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadPoolConfig {

    /**
     * 核心线程数
     */
    @Builder.Default
    private int corePoolSize = 5;

    /**
     * 最大线程数
     */
    @Builder.Default
    private int maxPoolSize = 20;

    /**
     * 队列容量
     */
    @Builder.Default
    private int queueCapacity = 100;

    /**
     * 线程空闲时间（秒）
     */
    @Builder.Default
    private long keepAliveTime = 60L;

    /**
     * 线程名前缀
     */
    @Builder.Default
    private String threadNamePrefix = "custom-pool-";

    /**
     * 是否允许核心线程超时
     */
    @Builder.Default
    private boolean allowCoreThreadTimeOut = true;

    /**
     * 等待任务完成后关闭线程池
     */
    @Builder.Default
    private boolean waitForTasksToCompleteOnShutdown = true;

    /**
     * 等待时间（秒）
     */
    @Builder.Default
    private int awaitTerminationSeconds = 60;

    /**
     * 拒绝策略
     */
    @Builder.Default
    private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

    /**
     * 创建默认配置
     */
    public static ThreadPoolConfig defaultConfig() {
        return ThreadPoolConfig.builder().build();
    }

    /**
     * 创建高性能配置
     */
    public static ThreadPoolConfig highPerformanceConfig() {
        return ThreadPoolConfig.builder()
                .corePoolSize(10)
                .maxPoolSize(50)
                .queueCapacity(200)
                .keepAliveTime(120L)
                .threadNamePrefix("high-perf-")
                .build();
    }

    /**
     * 创建低延迟配置
     */
    public static ThreadPoolConfig lowLatencyConfig() {
        return ThreadPoolConfig.builder()
                .corePoolSize(20)
                .maxPoolSize(100)
                .queueCapacity(50)
                .keepAliveTime(30L)
                .threadNamePrefix("low-latency-")
                .build();
    }

    /**
     * 创建CPU密集型配置
     */
    public static ThreadPoolConfig cpuIntensiveConfig() {
        int processors = Runtime.getRuntime().availableProcessors();
        return ThreadPoolConfig.builder()
                .corePoolSize(processors)
                .maxPoolSize(processors * 2)
                .queueCapacity(100)
                .keepAliveTime(60L)
                .threadNamePrefix("cpu-intensive-")
                .build();
    }

    /**
     * 创建IO密集型配置
     */
    public static ThreadPoolConfig ioIntensiveConfig() {
        int processors = Runtime.getRuntime().availableProcessors();
        return ThreadPoolConfig.builder()
                .corePoolSize(processors * 2)
                .maxPoolSize(processors * 4)
                .queueCapacity(200)
                .keepAliveTime(60L)
                .threadNamePrefix("io-intensive-")
                .build();
    }
}
