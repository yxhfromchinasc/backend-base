package com.lvxin.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lvxin.base.common.PageResult;
import com.lvxin.base.constant.RoleStatus;
import com.lvxin.base.repository.RoleRepository;
import com.lvxin.base.repository.entity.RoleDO;
import com.lvxin.base.service.RoleService;
import com.lvxin.base.service.bo.RoleBO;
import com.lvxin.base.util.JsonUtil;
import com.lvxin.base.web.converter.RoleConverter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色服务实现类
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Resource
    private RoleRepository roleRepository;

    @Override
    public RoleBO createRole(RoleBO roleBO) {
        // 规范化编码（大写）
        if (roleBO.getRoleCode() != null) {
            roleBO.setRoleCode(roleBO.getRoleCode().trim().toUpperCase());
        }
        // 唯一性校验
        RoleDO exists = roleRepository.selectOne(
                new QueryWrapper<RoleDO>().eq("role_code", roleBO.getRoleCode())
        );
        if (exists != null) {
            throw new RuntimeException("角色编码已存在");
        }

        RoleDO roleDO = RoleConverter.boToDo(roleBO);
        if ("SUPER_ADMIN".equals(roleDO.getRoleCode()) || "USER".equals(roleDO.getRoleCode())) {
            roleDO.setIsSystem(1);
        } else if (roleDO.getIsSystem() == null) {
            roleDO.setIsSystem(0);
        }
        roleDO.setStatus(RoleStatus.ENABLED.getCode());
        roleRepository.insert(roleDO);

        return RoleConverter.doToBo(roleDO);
    }

    @Override
    public RoleBO updateRole(Long roleId, RoleBO roleBO) {
        RoleDO roleDO = roleRepository.selectById(roleId);
        if (roleDO == null) {
            throw new RuntimeException("角色不存在");
        }
        // 系统角色禁止修改
        if (roleDO.getIsSystem() != null && roleDO.getIsSystem() == 1) {
            throw new RuntimeException("系统角色不允许修改");
        }

        // 应用其他字段更新
        RoleConverter.updateDoFromBo(roleDO, roleBO);

        // 如请求中包含角色编码，则规范化并做唯一性校验（排除自身）
        if (roleBO.getRoleCode() != null) {
            String normalized = roleBO.getRoleCode().trim().toUpperCase();
            RoleDO exists = roleRepository.selectOne(
                    new QueryWrapper<RoleDO>()
                            .eq("role_code", normalized)
                            .ne("id", roleDO.getId())
            );
            if (exists != null) {
                throw new RuntimeException("角色编码已存在");
            }
            roleDO.setRoleCode(normalized);
        }
        roleRepository.updateById(roleDO);

        return RoleConverter.doToBo(roleDO);
    }

    @Override
    public void deleteRole(String roleCode) {
        String normalized = roleCode == null ? null : roleCode.trim().toUpperCase();
        RoleDO roleDO = roleRepository.selectOne(
                new QueryWrapper<RoleDO>().eq("role_code", normalized)
        );
        if (roleDO == null) {
            throw new RuntimeException("角色不存在");
        }
        // 系统角色禁止删除
        if (roleDO.getIsSystem() != null && roleDO.getIsSystem() == 1) {
            throw new RuntimeException("系统角色不允许删除");
        }

        roleRepository.deleteById(roleDO.getId());
    }

    @Override
    public RoleBO getRoleById(Long roleId) {
        RoleDO roleDO = roleRepository.selectById(roleId);
        if (roleDO == null) {
            return null;
        }

        return RoleConverter.doToBo(roleDO);
    }

    @Override
    public RoleBO getRoleByCode(String roleCode) {
        String normalized = roleCode == null ? null : roleCode.trim().toUpperCase();
        RoleDO roleDO = roleRepository.selectOne(
                new QueryWrapper<RoleDO>().eq("role_code", normalized)
        );
        if (roleDO == null) {
            return null;
        }

        return RoleConverter.doToBo(roleDO);
    }

    @Override
    public PageResult<RoleBO> getRoleList(int page, int size, String keyword) {
        Page<RoleDO> pageParam = new Page<>(page, size);
        QueryWrapper<RoleDO> queryWrapper = new QueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("role_name", keyword)
                    .or()
                    .like("role_code", keyword);
        }

        Page<RoleDO> result = roleRepository.selectPage(pageParam, queryWrapper);

        List<RoleBO> records = result.getRecords().stream()
                .map(RoleConverter::doToBo)
                .toList();

        PageResult<RoleBO> pageResult = new PageResult<>();
        pageResult.setList(records);
        pageResult.setTotal(result.getTotal());
        pageResult.setPage((int) result.getCurrent());
        pageResult.setPageSize((int) result.getSize());
        return pageResult;
    }

    @Override
    public List<RoleBO> getAllRoles() {
        List<RoleDO> roleDOList = roleRepository.selectList(null);
        return roleDOList.stream()
                .map(RoleConverter::doToBo)
                .toList();
    }

    @Override
    public void assignPermissionsToRole(String roleCode, List<String> permissions) {

        RoleDO roleDO = roleRepository.selectOne(
                new QueryWrapper<RoleDO>().eq("role_code", roleCode)
        );
        if (roleDO == null) {
            throw new RuntimeException("角色不存在");
        }

        String permissionsJson = JsonUtil.toJson(permissions);
        roleDO.setPermissions(permissionsJson);
        roleRepository.updateById(roleDO);
    }

    @Override
    public List<String> getRolePermissions(String roleCode) {
        String normalized = roleCode == null ? null : roleCode.trim().toUpperCase();
        RoleDO roleDO = roleRepository.selectOne(
                new QueryWrapper<RoleDO>().eq("role_code", normalized)
        );
        if (roleDO == null || roleDO.getPermissions() == null) {
            return List.of();
        }

        return JsonUtil.toList(roleDO.getPermissions(), String.class);
    }

    @Override
    public void enableRole(String roleCode) {
        log.info("启用角色: {}", roleCode);

        String normalized = roleCode == null ? null : roleCode.trim().toUpperCase();
        RoleDO roleDO = roleRepository.selectOne(
                new QueryWrapper<RoleDO>().eq("role_code", normalized)
        );
        if (roleDO == null) {
            throw new RuntimeException("角色不存在");
        }
        // 系统角色允许启用/禁用，但不可被停用的是超级管理员（已有判断），此处保持逻辑不变

        // 检查是否为超级管理员角色，超级管理员角色不可停用
        if ("SUPER_ADMIN".equals(roleDO.getRoleCode())) {
            throw new RuntimeException("超级管理员角色不可停用");
        }

        roleDO.setStatus(RoleStatus.ENABLED.getCode());
        roleRepository.updateById(roleDO);

        log.info("角色 {} 已启用", roleCode);
    }

    @Override
    public void disableRole(String roleCode) {
        log.info("禁用角色: {}", roleCode);

        String normalized = roleCode == null ? null : roleCode.trim().toUpperCase();
        RoleDO roleDO = roleRepository.selectOne(
                new QueryWrapper<RoleDO>().eq("role_code", normalized)
        );
        if (roleDO == null) {
            throw new RuntimeException("角色不存在");
        }
        // 系统角色允许启用/禁用，但不可被停用的是超级管理员（已有判断），此处保持逻辑不变

        // 检查是否为超级管理员角色，超级管理员角色不可停用
        if ("SUPER_ADMIN".equals(roleDO.getRoleCode())) {
            throw new RuntimeException("超级管理员角色不可停用");
        }

        roleDO.setStatus(RoleStatus.DISABLED.getCode());
        roleRepository.updateById(roleDO);

        log.info("角色 {} 已禁用", roleCode);
    }
}
