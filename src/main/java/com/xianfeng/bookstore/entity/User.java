package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 t_user
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String email;

    private String address;

    /** 角色ID：1游客 2注册读者 3普通管理员 4超级管理员 */
    private Long roleId;

    /** 状态：1正常 0冻结 */
    private Integer status;

    private LocalDateTime createTime;
}
