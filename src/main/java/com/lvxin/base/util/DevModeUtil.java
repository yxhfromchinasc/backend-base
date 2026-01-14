package com.lvxin.base.util;

import com.lvxin.base.config.DevConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 开发模式工具类
 * 提供开发模式下的各种便利方法
 */
@Slf4j
@Component
public class DevModeUtil {

    @Autowired
    private DevConfig devConfig;

    /**
     * 检查是否启用开发模式
     *
     * @return 是否启用开发模式
     */
    public boolean isDevMode() {
        return devConfig.isDevMode();
    }

    /**
     * 开发模式日志记录
     *
     * @param message 日志消息
     */
    public void logDevMode(String message) {
        devConfig.logDevMode(message);
    }

    /**
     * 获取模拟的短信验证码
     *
     * @return 模拟的短信验证码
     */
    public String getMockSmsCode() {
        return devConfig.getMockSmsCode();
    }

    /**
     * 获取模拟的邮件验证码
     *
     * @return 模拟的邮件验证码
     */
    public String getMockEmailCode() {
        return devConfig.getMockEmailCode();
    }

    /**
     * 开发模式下的条件执行
     *
     * @param devAction  开发模式下的操作
     * @param prodAction 生产模式下的操作
     */
    public void execute(DevAction devAction, ProdAction prodAction) {
        if (isDevMode()) {
            logDevMode("执行开发模式操作");
            if (devAction != null) {
                devAction.execute();
            }
        } else {
            if (prodAction != null) {
                prodAction.execute();
            }
        }
    }

    /**
     * 开发模式操作接口
     */
    @FunctionalInterface
    public interface DevAction {
        void execute();
    }

    /**
     * 生产模式操作接口
     */
    @FunctionalInterface
    public interface ProdAction {
        void execute();
    }
}
