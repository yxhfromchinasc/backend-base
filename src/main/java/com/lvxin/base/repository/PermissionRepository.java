package com.lvxin.base.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvxin.base.repository.entity.PermissionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限Repository接口
 */
@Mapper
public interface PermissionRepository extends BaseMapper<PermissionDO> {

}
