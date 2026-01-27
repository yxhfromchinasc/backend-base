package com.lvxin.base.ai.monitor;

/**
 * 性能监控上下文
 * 使用ThreadLocal在线程间传递PerformanceMonitor
 *
 * @author lvxin
 */
public class PerformanceMonitorContext {
    
    private static final ThreadLocal<PerformanceMonitor> MONITOR_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前线程的监控器
     */
    public static void setMonitor(PerformanceMonitor monitor) {
        MONITOR_HOLDER.set(monitor);
    }
    
    /**
     * 获取当前线程的监控器
     */
    public static PerformanceMonitor getMonitor() {
        return MONITOR_HOLDER.get();
    }
    
    /**
     * 清除当前线程的监控器
     */
    public static void clear() {
        MONITOR_HOLDER.remove();
    }
    
    /**
     * 记录阶段（如果存在监控器）
     */
    public static void recordStage(String stage) {
        PerformanceMonitor monitor = getMonitor();
        if (monitor != null) {
            monitor.recordStage(stage);
        }
    }
    
    /**
     * 记录耗时（如果存在监控器）
     */
    public static void recordDuration(String stage, long duration) {
        PerformanceMonitor monitor = getMonitor();
        if (monitor != null) {
            monitor.recordDuration(stage, duration);
        }
    }
}
