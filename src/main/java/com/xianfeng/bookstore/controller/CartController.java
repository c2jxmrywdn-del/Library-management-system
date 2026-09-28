package com.xianfeng.bookstore.controller;

import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.entity.User;
import com.xianfeng.bookstore.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /** 加入购物车 */
    @PostMapping("/add")
    public R<Void> add(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Integer quantity = body.get("quantity") == null ? 1 : Integer.valueOf(body.get("quantity").toString());
        cartService.add(user.getUserId(), bookId, quantity);
        return R.ok();
    }

    /** 修改数量 */
    @PutMapping("/{cartId}")
    public R<Void> updateQuantity(@PathVariable Long cartId,
                                  @RequestParam Integer quantity,
                                  HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        cartService.updateQuantity(cartId, user.getUserId(), quantity);
        return R.ok();
    }

    /** 删除 */
    @DeleteMapping("/{cartId}")
    public R<Void> delete(@PathVariable Long cartId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        cartService.delete(cartId, user.getUserId());
        return R.ok();
    }

    /** 我的购物车 */
    @GetMapping("/my")
    public R<List<Map<String, Object>>> my(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return R.ok(cartService.myCart(user.getUserId()));
    }
}
