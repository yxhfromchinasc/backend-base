package com.lvxin.base.service.bo;

import com.lvxin.base.constant.RoleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色BO - Service层业务对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleBO {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private List<String> permissions;

    private RoleStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
