package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细表 t_order_item
 * 冗余存储下单时的书名与单价，保证历史订单不受后续改价影响
 */
@Data
@TableName("t_order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    private Long orderId;

    private Long bookId;

    /** 下单时冗余书名 */
    private String bookName;

    /** 下单时冗余单价 */
    private BigDecimal price;

    private Integer quantity;

    private BigDecimal subtotal;
}
