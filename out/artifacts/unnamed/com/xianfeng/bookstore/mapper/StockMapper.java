package com.xianfeng.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xianfeng.bookstore.entity.Stock;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 库存 Mapper
 */
public interface StockMapper extends BaseMapper<Stock> {

    /** 库存预警：库存数量低于预警阈值的图书 */
    @Select("select s.stock_id as stockId, s.book_id as bookId, b.book_name as bookName, " +
            "s.quantity, s.warn_quantity as warnQuantity " +
            "from t_stock s left join t_book b on s.book_id = b.book_id " +
            "where s.quantity <= s.warn_quantity " +
            "order by s.quantity asc")
    List<Map<String, Object>> selectWarnStock();
}
