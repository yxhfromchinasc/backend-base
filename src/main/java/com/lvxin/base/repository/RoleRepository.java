package com.lvxin.base.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvxin.base.repository.entity.RoleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色Repository接口
 */
@Mapper
public interface RoleRepository extends BaseMapper<RoleDO> {

}
