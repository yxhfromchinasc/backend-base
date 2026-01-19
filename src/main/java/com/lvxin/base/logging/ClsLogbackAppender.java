package com.lvxin.base.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.tencentcloudapi.cls.producer.AsyncProducerClient;
import com.tencentcloudapi.cls.producer.AsyncProducerConfig;
import com.tencentcloudapi.cls.producer.common.LogContent;
import com.tencentcloudapi.cls.producer.common.LogItem;
import com.tencentcloudapi.cls.producer.util.NetworkUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 腾讯云 CLS Logback Appender
 * 将日志异步上传到腾讯云 CLS
 */
@Slf4j
@Getter
@Setter
public class ClsLogbackAppender extends AppenderBase<ILoggingEvent> {

    private String endpoint;
    private String secretId;
    private String secretKey;
    private String topicId;
    private Integer batchSize = 100;
    private Integer lingerMs = 2000;
    private Integer maxRetries = 3;
    private Boolean enabled = true; // 默认启用，可通过配置禁用

    private AsyncProducerClient client;

    @Override
    public void start() {
        // 如果未启用，直接返回，不启动CLS客户端
        if (enabled == null || !enabled) {
            log.info("CLS日志上传已禁用，跳过初始化");
            return;
        }
        
        if (endpoint == null || endpoint.isEmpty()) {
            String errorMsg = "CLS endpoint is required but not configured";
            addError(errorMsg);
            return;
        }
        if (secretId == null || secretId.isEmpty()) {
            String errorMsg = "CLS secretId is required but not configured";
            addError(errorMsg);
            return;
        }
        if (secretKey == null || secretKey.isEmpty()) {
            String errorMsg = "CLS secretKey is required but not configured";
            addError(errorMsg);
            return;
        }
        if (topicId == null || topicId.isEmpty()) {
            String errorMsg = "CLS topicId is required but not configured";
            addError(errorMsg);
            return;
        }

        try {
            String localIp = NetworkUtils.getLocalMachineIP();
            AsyncProducerConfig config = new AsyncProducerConfig(endpoint, secretId, secretKey, localIp);

            client = new AsyncProducerClient(config);
            super.start();
            
            String successMsg = String.format("CLS Logback Appender started successfully, endpoint: %s, topicId: %s", endpoint, topicId);
            log.info(successMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to start CLS Logback Appender: " + e.getMessage();
            addError(errorMsg, e);
            log.error(errorMsg, e);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }
        
        if (client == null) {
            return;
        }

        try {
            LogItem logItem = new LogItem((int) (System.currentTimeMillis() / 1000));

            // 添加消息内容
            logItem.PushBack(new LogContent("content", event.getFormattedMessage()));

            // 添加日志级别
            logItem.PushBack(new LogContent("level", event.getLevel().toString()));

            // 添加日志器名称
            logItem.PushBack(new LogContent("logger", event.getLoggerName()));

            // 添加线程名
            logItem.PushBack(new LogContent("thread", event.getThreadName()));

            // 添加时间戳
            logItem.PushBack(new LogContent("timestamp", String.valueOf(event.getTimeStamp())));

            // 添加 MDC 上下文信息
            Map<String, String> mdc = MDC.getCopyOfContextMap();
            if (mdc != null && !mdc.isEmpty()) {
                for (Map.Entry<String, String> entry : mdc.entrySet()) {
                    logItem.PushBack(new LogContent(entry.getKey(), entry.getValue()));
                }
            }

            // 添加异常堆栈信息
            if (event.getThrowableProxy() != null) {
                logItem.PushBack(new LogContent("exception", event.getThrowableProxy().getMessage()));
                String stackTrace = getStackTrace(event.getThrowableProxy());
                if (stackTrace != null && !stackTrace.isEmpty()) {
                    logItem.PushBack(new LogContent("stackTrace", stackTrace));
                }
            }

            // 异步上传日志
            List<LogItem> logItems = new ArrayList<>();
            logItems.add(logItem);
            
            // 提取日志信息用于回调中记录
            final String logLevel = event.getLevel().toString();
            final String loggerName = event.getLoggerName();
            final String threadName = event.getThreadName();
            
            // 每次投递日志时记录日志
            log.debug("投递日志到CLS，topicId: {}, logLevel: {}, logger: {}, thread: {}", 
                    topicId, logLevel, loggerName, threadName);
            
            client.putLogs(topicId, logItems, result -> {
                if (result == null) {
                    log.error("CLS 回调结果为 null");
                    return;
                }
                
                try {
                    if (!result.isSuccessful()) {
                        String errorMsg = result.getErrorMessage();
                        log.error("Failed to upload log to CLS: {}", errorMsg);
                    }
                } catch (Exception e) {
                    log.error("处理 CLS 回调结果异常", e);
                }
            });

        } catch (Exception e) {
            // 避免日志上传失败影响应用运行
            addError("Failed to append log to CLS", e);
            log.error("处理日志异常", e);
        }
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(ch.qos.logback.classic.spi.IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(throwableProxy.getClassName()).append(": ").append(throwableProxy.getMessage()).append("\n");
        ch.qos.logback.classic.spi.IThrowableProxy cause = throwableProxy.getCause();
        int depth = 0;
        while (cause != null && depth < 10) { // 限制深度避免过长
            sb.append("Caused by: ").append(cause.getClassName()).append(": ").append(cause.getMessage()).append("\n");
            cause = cause.getCause();
            depth++;
        }
        return sb.toString();
    }

    @Override
    public void stop() {
        super.stop();
        if (client != null) {
            try {
                client.close();
                log.info("CLS Logback Appender stopped");
            } catch (Exception e) {
                addError("Failed to close CLS client", e);
            }
        }
    }
}

