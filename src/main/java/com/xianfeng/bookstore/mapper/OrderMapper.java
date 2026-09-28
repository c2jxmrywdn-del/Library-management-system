package com.xianfeng.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xianfeng.bookstore.entity.BookOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper
 */
public interface OrderMapper extends BaseMapper<BookOrder> {

    /** 销售统计：按日统计销售额与订单数 */
    @Select("select date_format(create_time, '%Y-%m-%d') as date, " +
            "count(*) as orderCount, sum(total_amount) as totalAmount " +
            "from t_order where order_status != 4 " +
            "group by date_format(create_time, '%Y-%m-%d') " +
            "order by date desc limit #{days}")
    List<Map<String, Object>> salesStats(@Param("days") int days);
}
