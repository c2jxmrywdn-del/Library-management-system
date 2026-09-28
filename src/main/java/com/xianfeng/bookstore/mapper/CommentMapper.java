package com.xianfeng.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xianfeng.bookstore.entity.Comment;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 评价 Mapper
 */
public interface CommentMapper extends BaseMapper<Comment> {

    /** 查询某图书已通过审核的评价（联表带用户名） */
    @Select("select c.comment_id as commentId, c.score, c.content, c.create_time as createTime, " +
            "u.username from t_comment c left join t_user u on c.user_id = u.user_id " +
            "where c.book_id = #{bookId} and c.audit_status = 1 " +
            "order by c.create_time desc")
    List<Map<String, Object>> selectByBookId(Long bookId);
}
