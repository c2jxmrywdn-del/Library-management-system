package com.xianfeng.bookstore.controller;

import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.entity.Book;
import com.xianfeng.bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 图书控制器：前台浏览/搜索/详情 + 后台图书管理
 */
@RestController
@RequestMapping("/book")
public class BookController {

    @Autowired
    private BookService bookService;

    // ============ 前台（游客可访问） ============

    /** 首页推荐 */
    @GetMapping("/home")
    public R<Map<String, Object>> home() {
        return R.ok(bookService.home());
    }

    /** 分页浏览/搜索图书 */
    @GetMapping("/page")
    public R<Map<String, Object>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "12") long size,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long categoryId) {
        return R.ok(bookService.pageBooks(page, size, keyword, categoryId));
    }

    /** 图书详情 */
    @GetMapping("/detail/{bookId}")
    public R<Map<String, Object>> detail(@PathVariable Long bookId) {
        return R.ok(bookService.detail(bookId));
    }

    // ============ 后台管理（/admin/book/**，需管理员） ============

    @GetMapping("/admin/page")
    public R<Map<String, Object>> adminPage(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword) {
        return R.ok(bookService.pageAll(page, size, keyword));
    }

    @PostMapping("/admin/add")
    public R<Void> add(@RequestBody Map<String, Object> body) {
        Book book = new Book();
        book.setBookName((String) body.get("bookName"));
        book.setAuthor((String) body.get("author"));
        book.setPublisher((String) body.get("publisher"));
        book.setIsbn((String) body.get("isbn"));
        book.setCategoryId(body.get("categoryId") == null ? null : Long.valueOf(body.get("categoryId").toString()));
        book.setDescription((String) body.get("description"));
        book.setCoverUrl((String) body.get("coverUrl"));
        if (body.get("price") != null) {
            book.setPrice(new java.math.BigDecimal(body.get("price").toString()));
        }
        if (body.get("discountPrice") != null) {
            book.setDiscountPrice(new java.math.BigDecimal(body.get("discountPrice").toString()));
        }
        Integer stock = body.get("stockQuantity") == null ? 0 : Integer.valueOf(body.get("stockQuantity").toString());
        bookService.addBook(book, stock);
        return R.ok();
    }

    @PutMapping("/admin/update")
    public R<Void> update(@RequestBody Book book) {
        bookService.updateBook(book);
        return R.ok();
    }

    /** 上架/下架：status=1上架 0下架 */
    @PutMapping("/admin/status")
    public R<Void> changeStatus(@RequestParam Long bookId, @RequestParam Integer status) {
        bookService.changeStatus(bookId, status);
        return R.ok();
    }
}
