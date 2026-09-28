package com.xianfeng.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xianfeng.bookstore.common.BusinessException;
import com.xianfeng.bookstore.entity.Book;
import com.xianfeng.bookstore.entity.Stock;
import com.xianfeng.bookstore.mapper.BookMapper;
import com.xianfeng.bookstore.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书服务：浏览、搜索、详情、后台维护、上下架
 */
@Service
public class BookService {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private StockMapper stockMapper;

    /** 前台分页查询图书（游客可访问），支持书名/作者/ISBN 模糊搜索与分类筛选 */
    public Map<String, Object> pageBooks(long page, long size, String keyword, Long categoryId) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, 1)
                .and(StringUtils.hasText(keyword), w -> w.like(Book::getBookName, keyword)
                        .or().like(Book::getAuthor, keyword)
                        .or().like(Book::getIsbn, keyword))
                .eq(categoryId != null, Book::getCategoryId, categoryId)
                .orderByDesc(Book::getCreateTime);
        Page<Book> p = bookMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("total", p.getTotal());
        result.put("list", p.getRecords());
        return result;
    }

    /** 图书详情（含库存） */
    public Map<String, Object> detail(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() == 0) {
            throw new BusinessException("图书不存在或已下架");
        }
        Stock stock = stockMapper.selectOne(
                new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, bookId));
        Map<String, Object> result = new HashMap<>();
        result.put("book", book);
        result.put("stock", stock == null ? 0 : stock.getQuantity());
        return result;
    }

    /** 首页：新书推荐 + 热销榜 */
    public Map<String, Object> home() {
        Map<String, Object> result = new HashMap<>();
        result.put("newBooks", bookMapper.selectNewBooks(8));
        result.put("hotBooks", bookMapper.selectHotBooks(8));
        return result;
    }

    /** 后台：新增图书（同时初始化库存） */
    public void addBook(Book book, Integer stockQuantity) {
        book.setCreateTime(LocalDateTime.now());
        if (book.getStatus() == null) {
            book.setStatus(1);
        }
        bookMapper.insert(book);
        // 初始化库存
        Stock stock = new Stock();
        stock.setBookId(book.getBookId());
        stock.setQuantity(stockQuantity == null ? 0 : stockQuantity);
        stock.setWarnQuantity(10);
        stock.setUpdateTime(LocalDateTime.now());
        stockMapper.insert(stock);
    }

    /** 后台：修改图书 */
    public void updateBook(Book book) {
        Book exist = bookMapper.selectById(book.getBookId());
        if (exist == null) {
            throw new BusinessException("图书不存在");
        }
        bookMapper.updateById(book);
    }

    /** 后台：上架/下架 */
    public void changeStatus(Long bookId, Integer status) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        book.setStatus(status);
        bookMapper.updateById(book);
    }

    /** 后台：全部分页（含下架图书） */
    public Map<String, Object> pageAll(long page, long size, String keyword) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), Book::getBookName, keyword)
                .orderByDesc(Book::getCreateTime);
        Page<Book> p = bookMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("total", p.getTotal());
        result.put("list", p.getRecords());
        return result;
    }
}
