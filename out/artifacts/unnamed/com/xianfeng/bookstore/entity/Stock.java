package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存表 t_stock
 */
@Data
@TableName("t_stock")
public class Stock {

    @TableId(type = IdType.AUTO)
    private Long stockId;

    private Long bookId;

    private Integer quantity;

    /** 库存预警阈值 */
    private Integer warnQuantity;

    private LocalDateTime updateTime;
}
