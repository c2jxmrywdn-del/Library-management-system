package com.xianfeng.bookstore.util;

/**
 * 角色常量（与数据库 t_role 对应）
 */
public class RoleConstants {
    /** 游客 */
    public static final Long GUEST = 1L;
    /** 注册读者 */
    public static final Long READER = 2L;
    /** 普通管理员（店员） */
    public static final Long STAFF = 3L;
    /** 超级管理员（店长） */
    public static final Long ADMIN = 4L;
}
