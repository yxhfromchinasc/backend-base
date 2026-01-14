package com.lvxin.base.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvxin.base.repository.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口 - Repository层
 */
@Mapper
public interface UserRepository extends BaseMapper<UserDO> {
    // MyBatis-Plus 提供了基础的CRUD操作
}