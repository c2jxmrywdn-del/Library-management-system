package com.xianfeng.bookstore.controller;

import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.entity.Stock;
import com.xianfeng.bookstore.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 库存控制器（后台）
 */
@RestController
@RequestMapping("/admin/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    /** 入库 */
    @PostMapping("/in")
    public R<Void> stockIn(@RequestParam Long bookId, @RequestParam Integer quantity) {
        stockService.stockIn(bookId, quantity);
        return R.ok();
    }

    /** 设置预警阈值 */
    @PutMapping("/warn")
    public R<Void> setWarn(@RequestParam Long bookId, @RequestParam Integer warnQuantity) {
        stockService.setWarn(bookId, warnQuantity);
        return R.ok();
    }

    /** 库存预警列表 */
    @GetMapping("/warn")
    public R<List<Map<String, Object>>> warn() {
        return R.ok(stockService.warnList());
    }

    /** 全部库存（盘点） */
    @GetMapping("/list")
    public R<List<Stock>> list() {
        return R.ok(stockService.list());
    }
}
