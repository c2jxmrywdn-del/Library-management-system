package com.xianfeng.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xianfeng.bookstore.common.BusinessException;
import com.xianfeng.bookstore.entity.*;
import com.xianfeng.bookstore.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 订单服务：下单、支付、取消、发货、退款
 */
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private CartService cartService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private StockMapper stockMapper;

    /** 从购物车结算下单 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkout(Long userId, String receiverName, String receiverPhone, String receiverAddress) {
        List<Cart> carts = cartService.listByUser(userId);
        if (carts == null || carts.isEmpty()) {
            throw new BusinessException("购物车为空，无法下单");
        }
        // 1. 校验库存并计算总额
        BigDecimal total = BigDecimal.ZERO;
        for (Cart c : carts) {
            Stock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, c.getBookId()));
            if (stock == null || stock.getQuantity() < c.getQuantity()) {
                Book b = bookMapper.selectById(c.getBookId());
                throw new BusinessException("图书《" + (b == null ? c.getBookId() : b.getBookName()) + "》库存不足");
            }
            Book book = bookMapper.selectById(c.getBookId());
            BigDecimal price = book.getDiscountPrice() != null ? book.getDiscountPrice() : book.getPrice();
            total = total.add(price.multiply(BigDecimal.valueOf(c.getQuantity())));
        }
        // 2. 生成订单主表
        BookOrder order = new BookOrder();
        order.setOrderNo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6));
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setPayStatus(0);
        order.setOrderStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        orderMapper.insert(order);

        // 3. 生成订单明细 + 扣减库存
        for (Cart c : carts) {
            Book book = bookMapper.selectById(c.getBookId());
            BigDecimal price = book.getDiscountPrice() != null ? book.getDiscountPrice() : book.getPrice();

            OrderItem item = new OrderItem();
            item.setOrderId(order.getOrderId());
            item.setBookId(c.getBookId());
            item.setBookName(book.getBookName());
            item.setPrice(price);
            item.setQuantity(c.getQuantity());
            item.setSubtotal(price.multiply(BigDecimal.valueOf(c.getQuantity())));
            orderItemMapper.insert(item);

            Stock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, c.getBookId()));
            stock.setQuantity(stock.getQuantity() - c.getQuantity());
            stock.setUpdateTime(LocalDateTime.now());
            stockMapper.updateById(stock);
        }
        // 4. 清空购物车
        cartService.clearByUser(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("totalAmount", total);
        return result;
    }

    /** 支付（模拟） */
    public void pay(String orderNo, Long userId) {
        BookOrder order = getMyOrder(orderNo, userId);
        if (order.getOrderStatus() != 0) {
            throw new BusinessException("订单状态不允许支付");
        }
        order.setPayStatus(1);
        order.setOrderStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /** 取消订单（恢复库存） */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo, Long userId) {
        BookOrder order = getMyOrder(orderNo, userId);
        if (order.getOrderStatus() != 0 && order.getOrderStatus() != 1) {
            throw new BusinessException("当前状态不能取消订单");
        }
        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getOrderId()));
        for (OrderItem item : items) {
            Stock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, item.getBookId()));
            if (stock != null) {
                stock.setQuantity(stock.getQuantity() + item.getQuantity());
                stock.setUpdateTime(LocalDateTime.now());
                stockMapper.updateById(stock);
            }
        }
        order.setOrderStatus(4);
        orderMapper.updateById(order);
    }

    /** 申请退款 */
    public void requestRefund(String orderNo, Long userId) {
        BookOrder order = getMyOrder(orderNo, userId);
        if (order.getOrderStatus() != 2) {
            throw new BusinessException("仅已发货订单可申请退款");
        }
        order.setOrderStatus(5);
        orderMapper.updateById(order);
    }

    /** 我的订单（含明细） */
    public Map<String, Object> myOrders(Long userId, long page, long size) {
        Page<BookOrder> p = orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BookOrder>().eq(BookOrder::getUserId, userId)
                        .orderByDesc(BookOrder::getCreateTime));
        Map<String, Object> result = new HashMap<>();
        result.put("total", p.getTotal());
        result.put("list", p.getRecords());
        return result;
    }

    /** 订单详情 */
    public Map<String, Object> orderDetail(String orderNo, Long userId) {
        BookOrder order = getMyOrder(orderNo, userId);
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getOrderId()));
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return result;
    }

    // ============ 后台 ============

    public Map<String, Object> adminPage(long page, long size, Integer orderStatus) {
        LambdaQueryWrapper<BookOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(orderStatus != null, BookOrder::getOrderStatus, orderStatus)
                .orderByDesc(BookOrder::getCreateTime);
        Page<BookOrder> p = orderMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("total", p.getTotal());
        result.put("list", p.getRecords());
        return result;
    }

    /** 后台：发货 */
    public void ship(Long orderId) {
        BookOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getOrderStatus() != 1) {
            throw new BusinessException("订单状态异常，无法发货");
        }
        order.setOrderStatus(2);
        orderMapper.updateById(order);
    }

    /** 后台：同意退款（恢复库存） */
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long orderId) {
        BookOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getOrderStatus() != 5) {
            throw new BusinessException("订单状态异常，无法退款");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            Stock stock = stockMapper.selectOne(
                    new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, item.getBookId()));
            if (stock != null) {
                stock.setQuantity(stock.getQuantity() + item.getQuantity());
                stock.setUpdateTime(LocalDateTime.now());
                stockMapper.updateById(stock);
            }
        }
        order.setOrderStatus(6);
        orderMapper.updateById(order);
    }

    private BookOrder getMyOrder(String orderNo, Long userId) {
        BookOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<BookOrder>().eq(BookOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人订单");
        }
        return order;
    }
}
