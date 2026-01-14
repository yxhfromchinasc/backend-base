package com.lvxin.base.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvxin.base.repository.entity.SystemConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置Repository - 数据访问层
 * 提供系统配置的数据库操作
 */
@Mapper
public interface SystemConfigRepository extends BaseMapper<SystemConfigDO> {

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @return 配置对象
     */
    SystemConfigDO selectByConfigKey(@Param("configKey") String configKey);

    /**
     * 查询所有公开的配置
     *
     * @return 公开配置列表
     */
    List<SystemConfigDO> selectPublicConfigs();
}

