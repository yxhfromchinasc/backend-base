package com.lvxin.base.service;

import com.lvxin.base.service.bo.SystemConfigBO;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 * 提供系统配置相关的业务逻辑
 */
public interface SystemConfigService {

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值（如果不存在返回null）
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置值（带默认值）
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在返回默认值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 根据配置键获取配置对象
     *
     * @param configKey 配置键
     * @return 配置对象
     */
    SystemConfigBO getConfig(String configKey);

    /**
     * 获取所有公开的配置（用户可见）
     *
     * @return 公开配置列表
     */
    List<SystemConfigBO> getPublicConfigs();

    /**
     * 获取所有公开的配置（以Map形式返回，key为configKey，value为configValue）
     *
     * @return 配置Map
     */
    Map<String, String> getPublicConfigsMap();

    /**
     * 设置配置值
     *
     * @param configKey 配置键
     * @param configValue 配置值
     * @param configType 配置类型（可选）
     * @param description 配置描述（可选）
     * @param isPublic 是否公开（可选）
     * @return 配置对象
     */
    SystemConfigBO setConfig(String configKey, String configValue, String configType, String description, Integer isPublic);

    /**
     * 更新配置值
     *
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 配置对象
     */
    SystemConfigBO updateConfigValue(String configKey, String configValue);

    /**
     * 删除配置
     *
     * @param configKey 配置键
     */
    void deleteConfig(String configKey);

    /**
     * 获取所有配置（管理员）
     *
     * @return 配置列表
     */
    List<SystemConfigBO> getAllConfigs();
}

