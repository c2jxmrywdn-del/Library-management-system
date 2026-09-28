package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表 t_order（类名用 BookOrder 避免与 SQL 关键字 order 冲突）
 */
@Data
@TableName("t_order")
public class BookOrder {

    @TableId(type = IdType.AUTO)
    private Long orderId;

    /** 订单编号（业务唯一） */
    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    /** 支付状态：0未支付 1已支付 */
    private Integer payStatus;

    /** 订单状态：0待付款 1待发货 2已发货 3已完成 4已取消 5退款中 6已退款 */
    private Integer orderStatus;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    /** 收货人信息 */
    private String receiverName;

    private String receiverPhone;

    private String receiverAddress;
}
