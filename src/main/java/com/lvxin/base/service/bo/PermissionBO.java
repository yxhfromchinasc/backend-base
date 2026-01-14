package com.lvxin.base.service.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限BO - Service层业务对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionBO {

    private Long id;

    private String permissionName;

    private String permissionCode;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
