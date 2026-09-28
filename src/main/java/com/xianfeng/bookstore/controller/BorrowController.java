package com.xianfeng.bookstore.controller;

import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.entity.User;
import com.xianfeng.bookstore.service.BorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 借阅控制器
 */
@RestController
@RequestMapping("/borrow")
public class BorrowController {

    @Autowired
    private BorrowService borrowService;

    /** 借书 */
    @PostMapping("/{bookId}")
    public R<Void> borrow(@PathVariable Long bookId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        borrowService.borrow(user.getUserId(), bookId);
        return R.ok();
    }

    /** 还书 */
    @PutMapping("/return/{borrowId}")
    public R<Void> giveBack(@PathVariable Long borrowId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        borrowService.giveBack(borrowId, user.getUserId());
        return R.ok();
    }

    /** 续借 */
    @PutMapping("/renew/{borrowId}")
    public R<Void> renew(@PathVariable Long borrowId, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        borrowService.renew(borrowId, user.getUserId());
        return R.ok();
    }

    /** 我的借阅 */
    @GetMapping("/my")
    public R<List<Map<String, Object>>> my(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        return R.ok(borrowService.myBorrows(user.getUserId()));
    }

    // ============ 后台 ============

    /** 借阅记录列表 */
    @GetMapping("/admin/borrow/list")
    public R<Map<String, Object>> adminList(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size) {
        return R.ok(borrowService.adminList(page, size));
    }

    /** 逾期未还列表 */
    @GetMapping("/admin/borrow/overdue")
    public R<List<Map<String, Object>>> overdue() {
        return R.ok(borrowService.overdueList());
    }
}
