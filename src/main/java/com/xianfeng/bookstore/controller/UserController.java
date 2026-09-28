package com.xianfeng.bookstore.controller;

import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.entity.User;
import com.xianfeng.bookstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

/**
 * 用户控制器：注册、登录、个人资料、后台读者管理
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /** 注册 */
    @PostMapping("/register")
    public R<Void> register(@RequestBody Map<String, String> body) {
        userService.register(body.get("username"), body.get("password"),
                body.get("realName"), body.get("phone"));
        return R.ok();
    }

    /** 登录，登录成功后将用户写入 session */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map<String, String> body, HttpSession session) {
        User user = userService.login(body.get("username"), body.get("password"));
        session.setAttribute("currentUser", user);
        return R.ok(user);
    }

    /** 登出 */
    @PostMapping("/logout")
    public R<Void> logout(HttpSession session) {
        session.removeAttribute("currentUser");
        return R.ok();
    }

    /** 当前登录用户 */
    @GetMapping("/info")
    public R<User> info(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return R.ok(user);
    }

    /** 更新个人资料 */
    @PutMapping("/info")
    public R<Void> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        userService.updateProfile(user.getUserId(), body.get("realName"),
                body.get("phone"), body.get("email"), body.get("address"));
        return R.ok();
    }

    // ============ 后台：读者管理 ============

    /** 分页查询读者 */
    @GetMapping("/admin/list")
    public R<Map<String, Object>> listReaders(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        return R.ok(userService.pageReaders(page, size, keyword));
    }

    /** 冻结/解冻 */
    @PutMapping("/admin/status")
    public R<Void> changeStatus(@RequestParam Long userId, @RequestParam Integer status) {
        userService.changeStatus(userId, status);
        return R.ok();
    }
}
