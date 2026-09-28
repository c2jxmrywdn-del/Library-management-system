package com.xianfeng.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xianfeng.bookstore.entity.Book;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 图书 Mapper
 */
public interface BookMapper extends BaseMapper<Book> {

    /** 热销图书排行：按销量统计前 N */
    @Select("select b.book_id as bookId, b.book_name as bookName, b.author, b.discount_price as price, " +
            "sum(oi.quantity) as soldCount " +
            "from t_order_item oi " +
            "left join t_book b on oi.book_id = b.book_id " +
            "group by oi.book_id " +
            "order by soldCount desc " +
            "limit #{topN}")
    List<Map<String, Object>> selectHotBooks(@Param("topN") int topN);

    /** 首页推荐：最新上架图书 */
    @Select("select * from t_book where status = 1 order by create_time desc limit #{topN}")
    List<Book> selectNewBooks(@Param("topN") int topN);
}
