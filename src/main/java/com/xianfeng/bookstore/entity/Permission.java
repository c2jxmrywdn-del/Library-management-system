package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 权限表 t_permission
 */
@Data
@TableName("t_permission")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long permId;

    private String permName;

    private String permUrl;

    private Long parentId;
}
