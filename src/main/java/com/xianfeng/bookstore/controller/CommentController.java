package com.xianfeng.bookstore.controller;

import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.entity.User;
import com.xianfeng.bookstore.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /** 读者发表评价 */
    @PostMapping("/add")
    public R<Void> add(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Integer score = Integer.valueOf(body.get("score").toString());
        commentService.add(user.getUserId(), bookId, score, (String) body.get("content"));
        return R.ok();
    }

    /** 查看某图书评价（游客可访问） */
    @GetMapping("/book/{bookId}")
    public R<List<Map<String, Object>>> byBook(@PathVariable Long bookId) {
        return R.ok(commentService.byBook(bookId));
    }

    /** 后台：分页查询 */
    @GetMapping("/admin/page")
    public R<Map<String, Object>> adminPage(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Integer auditStatus) {
        return R.ok(commentService.adminPage(page, size, auditStatus));
    }

    /** 后台：审核（1通过 2驳回） */
    @PutMapping("/admin/audit")
    public R<Void> audit(@RequestParam Long commentId, @RequestParam Integer auditStatus) {
        commentService.audit(commentId, auditStatus);
        return R.ok();
    }
}
