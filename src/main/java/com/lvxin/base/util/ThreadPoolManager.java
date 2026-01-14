package com.lvxin.base.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 线程池管理器
 * 统一管理所有线程池实例
 */
@Slf4j
@Component
public class ThreadPoolManager {

    private final Map<String, AsyncExecutor> executors = new ConcurrentHashMap<>();

    /**
     * 获取或创建默认线程池
     *
     * @return FlexibleExecutor
     */
    public AsyncExecutor defaultExecutor() {
        return getOrCreateExecutor("default", ThreadPoolConfig.defaultConfig());
    }

    /**
     * 获取或创建默认线程池
     *
     * @param name 线程池名称
     * @return FlexibleExecutor
     */
    public AsyncExecutor getOrCreateExecutor(String name) {
        return getOrCreateExecutor(name, ThreadPoolConfig.defaultConfig());
    }

    /**
     * 获取或创建自定义配置线程池
     *
     * @param name   线程池名称
     * @param config 线程池配置
     * @return FlexibleExecutor
     */
    public AsyncExecutor getOrCreateExecutor(String name, ThreadPoolConfig config) {
        return executors.computeIfAbsent(name, k -> {
            log.info("创建新线程池: {}", name);
            return new AsyncExecutor(config);
        });
    }

    /**
     * 获取高性能线程池
     *
     * @param name 线程池名称
     * @return FlexibleExecutor
     */
    public AsyncExecutor getHighPerformanceExecutor(String name) {
        return getOrCreateExecutor(name, ThreadPoolConfig.highPerformanceConfig());
    }

    /**
     * 获取低延迟线程池
     *
     * @param name 线程池名称
     * @return FlexibleExecutor
     */
    public AsyncExecutor getLowLatencyExecutor(String name) {
        return getOrCreateExecutor(name, ThreadPoolConfig.lowLatencyConfig());
    }

    /**
     * 获取CPU密集型线程池
     *
     * @param name 线程池名称
     * @return FlexibleExecutor
     */
    public AsyncExecutor getCpuIntensiveExecutor(String name) {
        return getOrCreateExecutor(name, ThreadPoolConfig.cpuIntensiveConfig());
    }

    /**
     * 获取IO密集型线程池
     *
     * @param name 线程池名称
     * @return FlexibleExecutor
     */
    public AsyncExecutor getIoIntensiveExecutor(String name) {
        return getOrCreateExecutor(name, ThreadPoolConfig.ioIntensiveConfig());
    }

    /**
     * 获取线程池
     *
     * @param name 线程池名称
     * @return FlexibleExecutor，如果不存在返回null
     */
    public AsyncExecutor getExecutor(String name) {
        return executors.get(name);
    }

    /**
     * 移除线程池
     *
     * @param name 线程池名称
     * @return 被移除的线程池
     */
    public AsyncExecutor removeExecutor(String name) {
        AsyncExecutor executor = executors.remove(name);
        if (executor != null) {
            log.info("移除线程池: {}", name);
            executor.shutdownGracefully(30);
        }
        return executor;
    }

    /**
     * 获取所有线程池状态
     *
     * @return 线程池状态信息
     */
    public String getAllStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 线程池状态总览 ===\n");
        executors.forEach((name, executor) -> {
            sb.append(executor.getStatus()).append("\n");
        });
        return sb.toString();
    }

    /**
     * 检查所有线程池健康状态
     *
     * @return 健康状态信息
     */
    public String getHealthStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 线程池健康检查 ===\n");
        executors.forEach((name, executor) -> {
            boolean healthy = executor.isHealthy();
            sb.append(String.format("线程池 %s: %s\n", name, healthy ? "健康" : "异常"));
        });
        return sb.toString();
    }

    /**
     * 关闭所有线程池
     */
    @PreDestroy
    public void shutdownAll() {
        log.info("开始关闭所有线程池，共 {} 个", executors.size());
        executors.forEach((name, executor) -> {
            try {
                executor.shutdownGracefully(30);
                log.info("线程池 {} 关闭完成", name);
            } catch (Exception e) {
                log.error("关闭线程池 {} 异常", name, e);
            }
        });
        executors.clear();
        log.info("所有线程池关闭完成");
    }

    /**
     * 获取线程池数量
     *
     * @return 线程池数量
     */
    public int getExecutorCount() {
        return executors.size();
    }

    /**
     * 检查线程池是否存在
     *
     * @param name 线程池名称
     * @return 是否存在
     */
    public boolean hasExecutor(String name) {
        return executors.containsKey(name);
    }
}
