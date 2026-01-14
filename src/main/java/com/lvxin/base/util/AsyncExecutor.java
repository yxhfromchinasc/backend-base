package com.lvxin.base.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 灵活的线程池执行器
 * 支持自定义配置或使用默认配置
 */
@Slf4j
public class AsyncExecutor {

    private final ThreadPoolTaskExecutor executor;
    private final String name;

    /**
     * 使用默认配置创建执行器
     */
    public AsyncExecutor() {
        this(ThreadPoolConfig.defaultConfig());
    }

    /**
     * 使用自定义配置创建执行器
     *
     * @param config 线程池配置
     */
    public AsyncExecutor(ThreadPoolConfig config) {
        this.name = config.getThreadNamePrefix() + System.currentTimeMillis();
        this.executor = createExecutor(config);
        log.info("创建线程池执行器: {}, 配置: 核心线程数={}, 最大线程数={}, 队列容量={}",
                name, config.getCorePoolSize(), config.getMaxPoolSize(), config.getQueueCapacity());
    }

    /**
     * 创建线程池执行器
     */
    private ThreadPoolTaskExecutor createExecutor(ThreadPoolConfig config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds((int) config.getKeepAliveTime());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setAllowCoreThreadTimeOut(config.isAllowCoreThreadTimeOut());
        executor.setWaitForTasksToCompleteOnShutdown(config.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(config.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(config.getRejectedExecutionHandler());

        executor.initialize();
        return executor;
    }

    /**
     * 异步执行任务（无返回值）
     *
     * @param task 要执行的任务
     */
    public void execute(Runnable task) {
        execute(task, null);
    }

    /**
     * 异步执行任务（无返回值，带异常处理）
     *
     * @param task             要执行的任务
     * @param exceptionHandler 异常处理器
     */
    public void execute(Runnable task, Consumer<Exception> exceptionHandler) {
        // 捕获当前线程的 MDC 上下文
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        
        executor.execute(() -> {
            // 恢复 MDC 上下文
            if (mdcContext != null && !mdcContext.isEmpty()) {
                MDC.setContextMap(mdcContext);
            }
            try {
                task.run();
            } catch (Exception e) {
                log.error("线程池 {} 执行任务异常", name, e);
                if (exceptionHandler != null) {
                    exceptionHandler.accept(e);
                }
            } finally {
                // 清理 MDC 上下文
                MDC.clear();
            }
        });
    }

    /**
     * 异步执行任务（有返回值）
     *
     * @param supplier 要执行的任务
     * @param <T>      返回值类型
     * @return CompletableFuture对象
     */
    public <T> CompletableFuture<T> execute(Supplier<T> supplier) {
        return execute(supplier, null);
    }

    /**
     * 异步执行任务（有返回值，带异常处理）
     *
     * @param supplier         要执行的任务
     * @param exceptionHandler 异常处理器
     * @param <T>              返回值类型
     * @return CompletableFuture对象
     */
    public <T> CompletableFuture<T> execute(Supplier<T> supplier, Function<Exception, T> exceptionHandler) {
        // 捕获当前线程的 MDC 上下文
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        
        return CompletableFuture.supplyAsync(() -> {
            // 恢复 MDC 上下文
            if (mdcContext != null && !mdcContext.isEmpty()) {
                MDC.setContextMap(mdcContext);
            }
            try {
                return supplier.get();
            } catch (Exception e) {
                log.error("线程池 {} 执行任务异常", name, e);
                if (exceptionHandler != null) {
                    return exceptionHandler.apply(e);
                }
                throw new RuntimeException(e);
            } finally {
                // 清理 MDC 上下文
                MDC.clear();
            }
        }, executor);
    }

    /**
     * 异步执行任务并获取结果（带超时）
     *
     * @param supplier 要执行的任务
     * @param timeout  超时时间
     * @param timeUnit 时间单位
     * @param <T>      返回值类型
     * @return 任务执行结果
     */
    public <T> T executeWithTimeout(Supplier<T> supplier, long timeout, TimeUnit timeUnit) {
        try {
            return execute(supplier).get(timeout, timeUnit);
        } catch (Exception e) {
            log.error("线程池 {} 执行任务超时或异常", name, e);
            throw new RuntimeException("任务执行失败", e);
        }
    }

    /**
     * 提交任务并获取Future
     *
     * @param task 要执行的任务
     * @return Future对象
     */
    public Future<?> submit(Runnable task) {
        // 捕获当前线程的 MDC 上下文
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        
        return executor.submit(() -> {
            // 恢复 MDC 上下文
            if (mdcContext != null && !mdcContext.isEmpty()) {
                MDC.setContextMap(mdcContext);
            }
            try {
                task.run();
            } finally {
                // 清理 MDC 上下文
                MDC.clear();
            }
        });
    }

    /**
     * 提交任务并获取Future
     *
     * @param task 要执行的任务
     * @param <T>  返回值类型
     * @return Future对象
     */
    public <T> Future<T> submit(Callable<T> task) {
        // 捕获当前线程的 MDC 上下文
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        
        return executor.submit(() -> {
            // 恢复 MDC 上下文
            if (mdcContext != null && !mdcContext.isEmpty()) {
                MDC.setContextMap(mdcContext);
            }
            try {
                return task.call();
            } finally {
                // 清理 MDC 上下文
                MDC.clear();
            }
        });
    }

    /**
     * 获取线程池状态信息
     *
     * @return 线程池状态信息
     */
    public String getStatus() {
        return String.format(
                "线程池 %s - 核心线程数: %d, 最大线程数: %d, 当前线程数: %d, 活跃线程数: %d, 队列大小: %d, 已完成任务数: %d",
                name,
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueueSize(),
                executor.getThreadPoolExecutor().getCompletedTaskCount()
        );
    }

    /**
     * 检查线程池是否健康
     *
     * @return 是否健康
     */
    public boolean isHealthy() {
        return !executor.getThreadPoolExecutor().isShutdown()
                && !executor.getThreadPoolExecutor().isTerminated();
    }

    /**
     * 安全关闭线程池
     *
     * @param timeout 超时时间（秒）
     */
    public void shutdownGracefully(int timeout) {
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(timeout);
        executor.shutdown();
        log.info("线程池 {} 开始安全关闭，超时时间: {}秒", name, timeout);
    }

    /**
     * 立即关闭线程池
     */
    public void shutdownNow() {
        executor.shutdown();
        log.info("线程池 {} 立即关闭", name);
    }

    /**
     * 获取底层执行器
     *
     * @return ThreadPoolTaskExecutor
     */
    public ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    /**
     * 获取线程池名称
     *
     * @return 线程池名称
     */
    public String getName() {
        return name;
    }
}
