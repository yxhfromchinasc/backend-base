package com.lvxin.base.service.impl;

import com.lvxin.base.exception.BusinessException;
import com.lvxin.base.repository.PermissionRepository;
import com.lvxin.base.repository.entity.PermissionDO;
import com.lvxin.base.service.PermissionService;
import com.lvxin.base.service.bo.PermissionBO;
import com.lvxin.base.web.converter.PermissionConverter;
import com.lvxin.base.web.req.QueryPermissionReq;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 */
@Slf4j
@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private PermissionRepository permissionRepository;

    @Override
    public PermissionBO createPermission(PermissionBO permissionBO) {
        log.info("创建权限: {}", permissionBO.getPermissionName());
        // 规范化编码（小写）
        if (permissionBO.getPermissionCode() != null) {
            permissionBO.setPermissionCode(permissionBO.getPermissionCode().trim().toLowerCase());
        }
        // 唯一性校验
        PermissionDO exists = permissionRepository.selectOne(
                new QueryWrapper<PermissionDO>().eq("permission_code", permissionBO.getPermissionCode())
        );
        if (exists != null) {
            throw new BusinessException("权限编码已存在");
        }

        PermissionDO permissionDO = PermissionConverter.boToDo(permissionBO);
        permissionRepository.insert(permissionDO);

        return PermissionConverter.doToBo(permissionDO);
    }

    @Override
    public PermissionBO updatePermission(PermissionBO permissionBO) {
        log.info("更新权限: {}", permissionBO.getPermissionCode());

        PermissionDO permissionDO = permissionRepository.selectById(permissionBO.getId());
        if (permissionDO == null) {
            throw new RuntimeException("权限不存在");
        }

        // 更新字段
        if (permissionBO.getPermissionName() != null) {
            permissionDO.setPermissionName(permissionBO.getPermissionName());
        }
        if (permissionBO.getPermissionCode() != null) {
            String normalized = permissionBO.getPermissionCode().trim().toLowerCase();
            // 唯一性校验（排除自身）
            PermissionDO exists = permissionRepository.selectOne(
                    new QueryWrapper<PermissionDO>()
                            .eq("permission_code", normalized)
                            .ne("id", permissionDO.getId())
            );
            if (exists != null) {
                throw new RuntimeException("权限编码已存在");
            }
            permissionDO.setPermissionCode(normalized);
        }
        if (permissionBO.getDescription() != null) {
            permissionDO.setDescription(permissionBO.getDescription());
        }

        permissionRepository.updateById(permissionDO);

        return PermissionConverter.doToBo(permissionDO);
    }

    @Override
    public PermissionBO getPermissionById(Long permissionId) {
        PermissionDO permissionDO = permissionRepository.selectById(permissionId);
        if (permissionDO == null) {
            return null;
        }

        return PermissionConverter.doToBo(permissionDO);
    }

    @Override
    public List<PermissionBO> getAllPermissions() {
        List<PermissionDO> permissionDOList = permissionRepository.selectList(null);
        return permissionDOList.stream()
                .map(PermissionConverter::doToBo)
                .collect(Collectors.toList());
    }

    @Override
    public PermissionBO getPermissionByCode(String permissionCode) {
        PermissionDO permissionDO = permissionRepository.selectOne(
                new QueryWrapper<PermissionDO>().eq("permission_code", permissionCode)
        );
        if (permissionDO == null) {
            return null;
        }

        return PermissionConverter.doToBo(permissionDO);
    }

    @Override
    public List<PermissionBO> getAllPermissions(QueryPermissionReq queryReq) {
        QueryWrapper<PermissionDO> queryWrapper = new QueryWrapper<>();

        if (queryReq.getPermissionName() != null && !queryReq.getPermissionName().trim().isEmpty()) {
            queryWrapper.like("permission_name", queryReq.getPermissionName().trim());
        }
        if (queryReq.getPermissionCode() != null && !queryReq.getPermissionCode().trim().isEmpty()) {
            queryWrapper.like("permission_code", queryReq.getPermissionCode().trim());
        }

        // 按创建时间倒序排列
        queryWrapper.orderByDesc("created_at");

        List<PermissionDO> permissionDOList = permissionRepository.selectList(queryWrapper);
        return permissionDOList.stream()
                .map(PermissionConverter::doToBo)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByCode(String permissionCode) {
        PermissionDO permission = permissionRepository.selectOne(
                new QueryWrapper<PermissionDO>().eq("permission_code", permissionCode)
        );
        if (permission == null) {
            return;
        }
        permissionRepository.deleteById(permission.getId());
    }
}
