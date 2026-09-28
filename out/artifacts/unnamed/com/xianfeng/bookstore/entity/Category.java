package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 图书分类表 t_category
 */
@Data
@TableName("t_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long categoryId;

    private String categoryName;

    private Long parentId;

    private Integer sortOrder;
}
