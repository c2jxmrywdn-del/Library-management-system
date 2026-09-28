package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅记录表 t_borrow
 * 状态：0借出中 1已归还 2逾期未还
 */
@Data
@TableName("t_borrow")
public class Borrow {

    @TableId(type = IdType.AUTO)
    private Long borrowId;

    private Long userId;

    private Long bookId;

    /** 借出时间 */
    private LocalDateTime borrowTime;

    /** 应还时间（默认借出后30天） */
    private LocalDateTime dueTime;

    /** 实际归还时间（null表示未还） */
    private LocalDateTime returnTime;

    /** 状态：0借出中 1已归还 2逾期未还 */
    private Integer status;

    /** 续借次数 */
    private Integer renewCount;

    /** 逾期罚款金额 */
    private BigDecimal fineAmount;
}
