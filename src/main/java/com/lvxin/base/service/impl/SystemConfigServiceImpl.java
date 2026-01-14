package com.lvxin.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.exception.ErrorCode;
import com.lvxin.base.repository.SystemConfigRepository;
import com.lvxin.base.repository.entity.SystemConfigDO;
import com.lvxin.base.service.SystemConfigService;
import com.lvxin.base.service.bo.SystemConfigBO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现类
 */
@Slf4j
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Resource
    private SystemConfigRepository systemConfigRepository;

    /**
     * 配置缓存（key: configKey, value: CacheItem）
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<String, CacheItem> configCache = new ConcurrentHashMap<>();

    /**
     * 缓存项
     */
    private static class CacheItem {
        private final String value;
        private final long expireTime;

        public CacheItem(String value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }

        public String getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 缓存过期时间（5分钟）
     */
    private static final long CACHE_TTL = 5 * 60 * 1000L;

    @Override
    public String getConfigValue(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            return null;
        }

        // 先从缓存读取
        CacheItem cacheItem = configCache.get(configKey);
        if (cacheItem != null && !cacheItem.isExpired()) {
            return cacheItem.getValue();
        }

        // 缓存未命中或已过期，查询数据库
        SystemConfigDO configDO = systemConfigRepository.selectByConfigKey(configKey);
        String value = configDO != null ? configDO.getConfigValue() : null;

        // 更新缓存（即使值为null也缓存，避免频繁查询数据库）
        if (value != null) {
            configCache.put(configKey, new CacheItem(value, System.currentTimeMillis() + CACHE_TTL));
        } else {
            // 值为null时，缓存一个特殊标记，避免重复查询不存在的配置
            configCache.put(configKey, new CacheItem(null, System.currentTimeMillis() + CACHE_TTL));
        }

        return value;
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public SystemConfigBO getConfig(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配置键不能为空");
        }

        // getConfig 方法需要返回完整的配置对象，不使用缓存
        // 因为配置对象包含多个字段，缓存整个对象比较复杂
        // 如果需要优化，可以单独缓存 SystemConfigBO
        SystemConfigDO configDO = systemConfigRepository.selectByConfigKey(configKey);
        if (configDO == null) {
            return null;
        }
        return convertToBO(configDO);
    }

    @Override
    public List<SystemConfigBO> getPublicConfigs() {
        List<SystemConfigDO> configDOList = systemConfigRepository.selectPublicConfigs();
        return configDOList.stream()
                .map(this::convertToBO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getPublicConfigsMap() {
        List<SystemConfigBO> configs = getPublicConfigs();
        Map<String, String> configMap = new HashMap<>();
        for (SystemConfigBO config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return configMap;
    }

    @Override
    @Transactional
    public SystemConfigBO setConfig(String configKey, String configValue, String configType, String description, Integer isPublic) {
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配置键不能为空");
        }

        SystemConfigDO configDO = systemConfigRepository.selectByConfigKey(configKey);
        if (configDO != null) {
            // 更新现有配置
            configDO.setConfigValue(configValue);
            if (configType != null) {
                configDO.setConfigType(configType);
            }
            if (description != null) {
                configDO.setDescription(description);
            }
            if (isPublic != null) {
                configDO.setIsPublic(isPublic);
            }
            systemConfigRepository.updateById(configDO);
            
            // 清除缓存
            evictCache(configKey);
            
            log.info("更新系统配置: configKey={}, configValue={}", configKey, configValue);
        } else {
            // 创建新配置
            configDO = new SystemConfigDO();
            configDO.setConfigKey(configKey);
            configDO.setConfigValue(configValue);
            configDO.setConfigType(configType != null ? configType : "string");
            configDO.setDescription(description);
            configDO.setIsPublic(isPublic != null ? isPublic : 0);
            systemConfigRepository.insert(configDO);
            
            // 新配置不需要清除缓存（因为之前没有缓存）
            
            log.info("创建系统配置: configKey={}, configValue={}", configKey, configValue);
        }

        return convertToBO(configDO);
    }

    @Override
    @Transactional
    public SystemConfigBO updateConfigValue(String configKey, String configValue) {
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配置键不能为空");
        }

        SystemConfigDO configDO = systemConfigRepository.selectByConfigKey(configKey);
        if (configDO == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在: " + configKey);
        }

        configDO.setConfigValue(configValue);
        systemConfigRepository.updateById(configDO);
        
        // 清除缓存
        evictCache(configKey);
        
        log.info("更新系统配置值: configKey={}, configValue={}", configKey, configValue);

        return convertToBO(configDO);
    }

    @Override
    @Transactional
    public void deleteConfig(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配置键不能为空");
        }

        SystemConfigDO configDO = systemConfigRepository.selectByConfigKey(configKey);
        if (configDO == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在: " + configKey);
        }

        systemConfigRepository.deleteById(configDO.getId());
        
        // 清除缓存
        evictCache(configKey);
        
        log.info("删除系统配置: configKey={}", configKey);
    }

    @Override
    public List<SystemConfigBO> getAllConfigs() {
        List<SystemConfigDO> configDOList = systemConfigRepository.selectList(new QueryWrapper<>());
        return configDOList.stream()
                .map(this::convertToBO)
                .collect(Collectors.toList());
    }

    /**
     * 将DO对象转换为BO对象
     */
    private SystemConfigBO convertToBO(SystemConfigDO configDO) {
        SystemConfigBO configBO = new SystemConfigBO();
        BeanUtils.copyProperties(configDO, configBO);
        return configBO;
    }

    /**
     * 清除指定配置的缓存
     * 
     * @param configKey 配置键
     */
    private void evictCache(String configKey) {
        if (configKey != null) {
            configCache.remove(configKey);
            log.debug("清除配置缓存: configKey={}", configKey);
        }
    }

    /**
     * 清除所有配置缓存（用于批量更新场景）
     */
    public void evictAllCache() {
        configCache.clear();
        log.info("清除所有配置缓存");
    }
}

