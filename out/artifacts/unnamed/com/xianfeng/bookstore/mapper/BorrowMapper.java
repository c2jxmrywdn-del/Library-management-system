package com.xianfeng.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xianfeng.bookstore.entity.Borrow;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 借阅 Mapper
 */
public interface BorrowMapper extends BaseMapper<Borrow> {

    /** 我的借阅记录（联表带出书名） */
    @Select("select b.borrow_id as borrowId, b.book_id as bookId, b.borrow_time as borrowTime, " +
            "b.due_time as dueTime, b.return_time as returnTime, b.status, b.renew_count as renewCount, " +
            "b.fine_amount as fineAmount, k.book_name as bookName " +
            "from t_borrow b left join t_book k on b.book_id = k.book_id " +
            "where b.user_id = #{userId} order by b.borrow_time desc")
    List<Map<String, Object>> selectMyBorrows(Long userId);

    /** 后台：全部借阅记录（联表带书名、用户名） */
    @Select("select b.borrow_id as borrowId, b.book_id as bookId, b.user_id as userId, " +
            "b.borrow_time as borrowTime, b.due_time as dueTime, b.return_time as returnTime, " +
            "b.status, b.renew_count as renewCount, b.fine_amount as fineAmount, " +
            "k.book_name as bookName, u.username " +
            "from t_borrow b " +
            "left join t_book k on b.book_id = k.book_id " +
            "left join t_user u on b.user_id = u.user_id " +
            "order by b.borrow_time desc limit #{offset}, #{size}")
    List<Map<String, Object>> selectAdminList(@org.apache.ibatis.annotations.Param("offset") long offset,
                                              @org.apache.ibatis.annotations.Param("size") long size);

    /** 逾期未还列表 */
    @Select("select b.borrow_id as borrowId, b.book_id as bookId, b.user_id as userId, " +
            "b.due_time as dueTime, k.book_name as bookName, u.username " +
            "from t_borrow b " +
            "left join t_book k on b.book_id = k.book_id " +
            "left join t_user u on b.user_id = u.user_id " +
            "where b.status = 2 order by b.due_time asc")
    List<Map<String, Object>> selectOverdue();
}
