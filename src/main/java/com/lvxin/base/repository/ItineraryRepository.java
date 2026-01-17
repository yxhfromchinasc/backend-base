package com.lvxin.base.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvxin.base.repository.entity.ItineraryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 计划表Repository - 数据访问层
 * 提供计划表的数据库操作
 */
@Mapper
public interface ItineraryRepository extends BaseMapper<ItineraryDO> {

    /**
     * 根据计划表唯一标识（Key）查询计划表
     *
     * @param itineraryKey 计划表唯一标识（Key）
     * @return 计划表对象
     */
    ItineraryDO selectByItineraryKey(@Param("itineraryKey") String itineraryKey);

    /**
     * 根据用户ID查询用户的所有计划表
     *
     * @param userId 用户ID
     * @return 计划表列表
     */
    List<ItineraryDO> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据计划表唯一标识（Key）查询所有版本（包括历史版本）
     *
     * @param itineraryKey 计划表唯一标识（Key）
     * @return 计划表列表（按版本号倒序）
     */
    List<ItineraryDO> selectAllVersionsByItineraryKey(@Param("itineraryKey") String itineraryKey);

    /**
     * 将指定计划表的所有版本的is_current_version设置为0
     *
     * @param itineraryKey 计划表唯一标识（Key）
     * @return 更新的记录数
     */
    int clearCurrentVersionFlag(@Param("itineraryKey") String itineraryKey);
}
