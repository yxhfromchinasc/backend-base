package com.lvxin.base.ai.monitor;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 性能监控器
 * 用于记录异步任务各个环节的耗时情况
 *
 * @author lvxin
 */
@Slf4j
public class PerformanceMonitor {
    
    private final String taskId;
    private final long startTime;
    private final Map<String, Long> timings = new ConcurrentHashMap<>();
    private final List<TimingRecord> records = new ArrayList<>();
    
    public PerformanceMonitor(String taskId) {
        this.taskId = taskId;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * 记录一个时间点
     *
     * @param stage 阶段名称
     * @return 当前时间戳
     */
    public long recordStage(String stage) {
        long currentTime = System.currentTimeMillis();
        timings.put(stage, currentTime);
        return currentTime;
    }
    
    /**
     * 记录一个时间段的耗时
     *
     * @param stage 阶段名称
     * @param duration 耗时（毫秒）
     */
    public void recordDuration(String stage, long duration) {
        records.add(new TimingRecord(stage, duration, System.currentTimeMillis()));
    }
    
    /**
     * 计算两个阶段之间的耗时
     *
     * @param startStage 开始阶段
     * @param endStage   结束阶段
     * @return 耗时（毫秒），如果阶段不存在返回-1
     */
    public long getDuration(String startStage, String endStage) {
        Long start = timings.get(startStage);
        Long end = timings.get(endStage);
        if (start == null || end == null) {
            return -1;
        }
        return end - start;
    }
    
    /**
     * 获取总耗时
     *
     * @return 总耗时（毫秒）
     */
    public long getTotalDuration() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 输出完整的性能报告
     */
    public void logPerformanceReport() {
        long totalDuration = getTotalDuration();
        
        log.info("========== [性能监控] {} ==========", taskId);
        log.info("总耗时: {}ms ({}s)", totalDuration, String.format("%.2f", totalDuration / 1000.0));
        
        // 统计分析
        analyzeAndLogStatistics(totalDuration);
        
        log.info("--- 各阶段耗时详情（按耗时排序） ---");
        
        // 按耗时排序输出记录的时间段
        records.stream()
                .sorted((a, b) -> Long.compare(b.duration, a.duration))
                .forEach(record -> {
                    double percentage = (record.duration * 100.0) / totalDuration;
                    if (record.duration > 100 || percentage > 0.1) { // 只显示耗时>100ms或占比>0.1%的
                        log.info("  [{}] {}ms ({}%)", record.stage, record.duration, String.format("%.2f", percentage));
                    }
                });
        
        log.info("========== [性能监控] 结束 ==========");
    }
    
    /**
     * 分析并输出统计信息
     */
    private void analyzeAndLogStatistics(long totalDuration) {
        // 统计各类操作的耗时
        long aiCallTotal = 0;
        long toolCallTotal = 0;
        long httpRequestTotal = 0;
        int aiCallCount = 0;
        int toolCallCount = 0;
        int httpRequestCount = 0;
        
        for (TimingRecord record : records) {
            String stage = record.stage.toLowerCase();
            if (stage.contains("deepseek http请求") || stage.contains("ai生成响应") || stage.contains("ai调用")) {
                aiCallTotal += record.duration;
                if (stage.contains("deepseek http请求")) {
                    httpRequestCount++;
                    httpRequestTotal += record.duration;
                }
                if (stage.contains("ai生成响应")) {
                    aiCallCount++;
                }
            } else if (stage.contains("工具调用") || stage.contains("执行工具")) {
                toolCallTotal += record.duration;
                if (stage.contains("所有工具调用总耗时")) {
                    toolCallCount++;
                }
            }
        }
        
        log.info("--- 性能统计 ---");
        log.info("  AI调用: {}次, 总耗时: {}ms ({}%)", 
                aiCallCount, aiCallTotal, String.format("%.2f", (aiCallTotal * 100.0) / totalDuration));
        log.info("  HTTP请求: {}次, 总耗时: {}ms ({}%), 平均: {}ms/次", 
                httpRequestCount, httpRequestTotal, String.format("%.2f", (httpRequestTotal * 100.0) / totalDuration),
                httpRequestCount > 0 ? String.format("%.2f", httpRequestTotal / (double) httpRequestCount) : "0");
        log.info("  工具调用: {}次, 总耗时: {}ms ({}%)", 
                toolCallCount, toolCallTotal, String.format("%.2f", (toolCallTotal * 100.0) / totalDuration));
        
        // 找出最慢的HTTP请求
        records.stream()
                .filter(r -> r.stage.contains("DeepSeek HTTP请求"))
                .sorted((a, b) -> Long.compare(b.duration, a.duration))
                .limit(1)
                .forEach(record -> {
                    double percentage = (record.duration * 100.0) / totalDuration;
                    log.warn("  ⚠️ 最慢的HTTP请求: {}ms ({}%) - {}", 
                            record.duration, String.format("%.2f", percentage), record.stage);
                });
        
        // 找出耗时最长的前3个阶段
        log.info("--- 耗时Top3 ---");
        records.stream()
                .sorted((a, b) -> Long.compare(b.duration, a.duration))
                .limit(3)
                .forEach(record -> {
                    double percentage = (record.duration * 100.0) / totalDuration;
                    log.info("  [{}] {}ms ({}%)", record.stage, record.duration, String.format("%.2f", percentage));
                });
    }
    
    /**
     * 输出简要的性能摘要
     */
    public void logPerformanceSummary() {
        long totalDuration = getTotalDuration();
        log.info("[{}] 性能摘要 - 总耗时: {}ms ({}s)", 
                taskId, totalDuration, String.format("%.2f", totalDuration / 1000.0));
        
        // 找出耗时最长的前3个阶段
        records.stream()
                .sorted((a, b) -> Long.compare(b.duration, a.duration))
                .limit(3)
                .forEach(record -> {
                    double percentage = (record.duration * 100.0) / totalDuration;
                    log.info("  [{}] {}ms ({}%)", record.stage, record.duration, String.format("%.2f", percentage));
                });
    }
    
    /**
     * 时间记录
     */
    private static class TimingRecord {
        final String stage;
        final long duration;
        final long timestamp;
        
        TimingRecord(String stage, long duration, long timestamp) {
            this.stage = stage;
            this.duration = duration;
            this.timestamp = timestamp;
        }
    }
}
