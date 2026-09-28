package com.xianfeng.bookstore.controller;

import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计分析控制器（后台）
 */
@RestController
@RequestMapping("/admin/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    /** 销售报表：近 N 天 */
    @GetMapping("/sales")
    public R<List<Map<String, Object>>> sales(@RequestParam(defaultValue = "30") int days) {
        return R.ok(statsService.salesReport(days));
    }

    /** 热销图书 Top N */
    @GetMapping("/hot")
    public R<List<Map<String, Object>>> hot(@RequestParam(defaultValue = "10") int topN) {
        return R.ok(statsService.hotBooks(topN));
    }

    /** 库存预警 */
    @GetMapping("/stockWarn")
    public R<List<Map<String, Object>>> stockWarn() {
        return R.ok(statsService.stockWarn());
    }

    /** 仪表盘概览 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(statsService.overview());
    }
}
