package com.xianfeng.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xianfeng.bookstore.entity.Cart;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 购物车 Mapper
 */
public interface CartMapper extends BaseMapper<Cart> {

    /** 查询某用户购物车（联表带出图书信息） */
    @Select("select c.cart_id as cartId, c.book_id as bookId, c.quantity, " +
            "b.book_name as bookName, b.author, b.discount_price as price, b.cover_url as coverUrl " +
            "from t_cart c left join t_book b on c.book_id = b.book_id " +
            "where c.user_id = #{userId}")
    List<Map<String, Object>> selectCartDetail(Long userId);
}
