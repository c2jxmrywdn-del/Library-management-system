package com.xianfeng.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xianfeng.bookstore.common.BusinessException;
import com.xianfeng.bookstore.entity.Comment;
import com.xianfeng.bookstore.mapper.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评价服务：发表、查看、后台审核
 */
@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    /** 读者发表评价（默认待审核） */
    public void add(Long userId, Long bookId, Integer score, String content) {
        if (score == null || score < 1 || score > 5) {
            throw new BusinessException("评分必须在1-5之间");
        }
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setBookId(bookId);
        comment.setScore(score);
        comment.setContent(content);
        comment.setCreateTime(LocalDateTime.now());
        comment.setAuditStatus(0);
        commentMapper.insert(comment);
    }

    /** 查看某图书的已通过评价 */
    public List<Map<String, Object>> byBook(Long bookId) {
        return commentMapper.selectByBookId(bookId);
    }

    /** 后台：分页查询评价（可按审核状态过滤） */
    public Map<String, Object> adminPage(long page, long size, Integer auditStatus) {
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(auditStatus != null, Comment::getAuditStatus, auditStatus)
                        .orderByDesc(Comment::getCreateTime));
        Map<String, Object> result = new HashMap<>();
        result.put("total", p.getTotal());
        result.put("list", p.getRecords());
        return result;
    }

    /** 后台：审核评价（通过/驳回） */
    public void audit(Long commentId, Integer auditStatus) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评价不存在");
        }
        comment.setAuditStatus(auditStatus);
        commentMapper.updateById(comment);
    }
}
