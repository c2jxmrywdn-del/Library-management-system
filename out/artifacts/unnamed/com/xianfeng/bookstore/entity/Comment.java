package com.xianfeng.bookstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价表 t_comment
 */
@Data
@TableName("t_comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long commentId;

    private Long userId;

    private Long bookId;

    /** 评分 1-5 */
    private Integer score;

    private String content;

    private LocalDateTime createTime;

    /** 审核状态：0待审核 1已通过 2已驳回 */
    private Integer auditStatus;
}
