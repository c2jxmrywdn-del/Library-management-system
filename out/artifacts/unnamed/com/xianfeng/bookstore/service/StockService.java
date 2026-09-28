package com.xianfeng.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xianfeng.bookstore.common.BusinessException;
import com.xianfeng.bookstore.entity.Stock;
import com.xianfeng.bookstore.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 库存服务：入库、预警、盘点
 */
@Service
public class StockService {

    @Autowired
    private StockMapper stockMapper;

    /** 入库：增加某图书库存 */
    public void stockIn(Long bookId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("入库数量必须大于0");
        }
        Stock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, bookId));
        if (stock == null) {
            stock = new Stock();
            stock.setBookId(bookId);
            stock.setQuantity(quantity);
            stock.setWarnQuantity(10);
            stock.setUpdateTime(LocalDateTime.now());
            stockMapper.insert(stock);
        } else {
            stock.setQuantity(stock.getQuantity() + quantity);
            stock.setUpdateTime(LocalDateTime.now());
            stockMapper.updateById(stock);
        }
    }

    /** 设置库存预警阈值 */
    public void setWarn(Long bookId, Integer warnQuantity) {
        Stock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, bookId));
        if (stock == null) {
            throw new BusinessException("该图书尚无库存记录");
        }
        stock.setWarnQuantity(warnQuantity);
        stockMapper.updateById(stock);
    }

    /** 库存预警列表 */
    public List<Map<String, Object>> warnList() {
        return stockMapper.selectWarnStock();
    }

    /** 全部库存（盘点） */
    public List<Stock> list() {
        return stockMapper.selectList(new LambdaQueryWrapper<>());
    }
}
