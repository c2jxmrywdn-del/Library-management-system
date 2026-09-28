package com.xianfeng.bookstore.service;

import com.xianfeng.bookstore.mapper.BookMapper;
import com.xianfeng.bookstore.mapper.OrderMapper;
import com.xianfeng.bookstore.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析服务：销售报表、热销榜、库存预警
 */
@Service
public class StatsService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private StockMapper stockMapper;

    /** 销售报表：近 N 天销售额与订单数 */
    public List<Map<String, Object>> salesReport(int days) {
        return orderMapper.salesStats(days);
    }

    /** 热销图书排行 Top N */
    public List<Map<String, Object>> hotBooks(int topN) {
        return bookMapper.selectHotBooks(topN);
    }

    /** 库存预警 */
    public List<Map<String, Object>> stockWarn() {
        return stockMapper.selectWarnStock();
    }

    /** 仪表盘概览 */
    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("sales30", orderMapper.salesStats(30));
        result.put("hotTop10", bookMapper.selectHotBooks(10));
        result.put("stockWarn", stockMapper.selectWarnStock());
        return result;
    }
}
