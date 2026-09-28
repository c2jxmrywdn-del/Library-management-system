package com.xianfeng.bookstore.controller;

import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.entity.User;
import com.xianfeng.bookstore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * 订单控制器：下单、支付、取消、退款 + 后台发货/退款
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /** 从购物车结算下单 */
    @PostMapping("/checkout")
    public R<Map<String, Object>> checkout(@RequestBody Map<String, String> body, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return R.ok(orderService.checkout(user.getUserId(),
                body.get("receiverName"), body.get("receiverPhone"), body.get("receiverAddress")));
    }

    /** 支付 */
    @PostMapping("/pay/{orderNo}")
    public R<Void> pay(@PathVariable String orderNo, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        orderService.pay(orderNo, user.getUserId());
        return R.ok();
    }

    /** 取消订单 */
    @PutMapping("/cancel/{orderNo}")
    public R<Void> cancel(@PathVariable String orderNo, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        orderService.cancel(orderNo, user.getUserId());
        return R.ok();
    }

    /** 申请退款 */
    @PutMapping("/refund/{orderNo}")
    public R<Void> refund(@PathVariable String orderNo, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        orderService.requestRefund(orderNo, user.getUserId());
        return R.ok();
    }

    /** 我的订单 */
    @GetMapping("/my")
    public R<Map<String, Object>> my(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size,
                                     HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return R.ok(orderService.myOrders(user.getUserId(), page, size));
    }

    /** 订单详情 */
    @GetMapping("/detail/{orderNo}")
    public R<Map<String, Object>> detail(@PathVariable String orderNo, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return R.ok(orderService.orderDetail(orderNo, user.getUserId()));
    }

    // ============ 后台 ============

    @GetMapping("/admin/page")
    public R<Map<String, Object>> adminPage(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Integer orderStatus) {
        return R.ok(orderService.adminPage(page, size, orderStatus));
    }

    /** 发货 */
    @PutMapping("/admin/ship/{orderId}")
    public R<Void> ship(@PathVariable Long orderId) {
        orderService.ship(orderId);
        return R.ok();
    }

    /** 同意退款 */
    @PutMapping("/admin/refund/{orderId}")
    public R<Void> approveRefund(@PathVariable Long orderId) {
        orderService.approveRefund(orderId);
        return R.ok();
    }
}
