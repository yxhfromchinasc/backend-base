package com.lvxin.base.web.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private List<String> permissions;

    private Integer status;

    private String createdAt;

    private String updatedAt;
}
