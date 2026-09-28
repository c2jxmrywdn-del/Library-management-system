package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色表 t_role
 */
@Data
@TableName("t_role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long roleId;

    private String roleName;

    private String roleDesc;
}
