package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书信息表 t_book
 */
@Data
@TableName("t_book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long bookId;

    private String bookName;

    private String author;

    private String publisher;

    private String isbn;

    private BigDecimal price;

    private BigDecimal discountPrice;

    private String coverUrl;

    private String description;

    private Long categoryId;

    /** 状态：1上架 0下架 */
    private Integer status;

    private LocalDateTime createTime;
}
