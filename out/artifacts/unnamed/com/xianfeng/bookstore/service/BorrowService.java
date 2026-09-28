package com.xianfeng.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xianfeng.bookstore.common.BusinessException;
import com.xianfeng.bookstore.entity.Book;
import com.xianfeng.bookstore.entity.Borrow;
import com.xianfeng.bookstore.entity.Stock;
import com.xianfeng.bookstore.mapper.BookMapper;
import com.xianfeng.bookstore.mapper.BorrowMapper;
import com.xianfeng.bookstore.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 借阅服务：借书、还书、续借、逾期统计
 * 借阅与销售共用 t_stock 库存：借出扣减、还书恢复
 */
@Service
public class BorrowService {

    @Autowired
    private BorrowMapper borrowMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private StockMapper stockMapper;

    /** 借阅天数 */
    private static final int BORROW_DAYS = 30;
    /** 续借天数 */
    private static final int RENEW_DAYS = 30;
    /** 最大续借次数 */
    private static final int MAX_RENEW = 2;
    /** 逾期每天罚款（元） */
    private static final BigDecimal FINE_PER_DAY = new BigDecimal("0.5");

    /** 读者借书 */
    @Transactional(rollbackFor = Exception.class)
    public void borrow(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() == 0) {
            throw new BusinessException("图书不存在或已下架");
        }
        // 同一图书未还清则不能再借
        Long borrowing = borrowMapper.selectCount(new LambdaQueryWrapper<Borrow>()
                .eq(Borrow::getUserId, userId).eq(Borrow::getBookId, bookId)
                .ne(Borrow::getStatus, 1));
        if (borrowing != null && borrowing > 0) {
            throw new BusinessException("该书您尚未归还，不能重复借阅");
        }
        // 校验库存
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, bookId));
        if (stock == null || stock.getQuantity() <= 0) {
            throw new BusinessException("《" + book.getBookName() + "》暂无可借库存");
        }
        // 扣库存
        stock.setQuantity(stock.getQuantity() - 1);
        stock.setUpdateTime(LocalDateTime.now());
        stockMapper.updateById(stock);

        // 生成借阅记录
        Borrow record = new Borrow();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowTime(LocalDateTime.now());
        record.setDueTime(LocalDateTime.now().plusDays(BORROW_DAYS));
        record.setStatus(0);
        record.setRenewCount(0);
        record.setFineAmount(BigDecimal.ZERO);
        borrowMapper.insert(record);
    }

    /** 读者还书 */
    @Transactional(rollbackFor = Exception.class)
    public void giveBack(Long borrowId, Long userId) {
        Borrow record = getMyBorrow(borrowId, userId);
        if (record.getStatus() == 1) {
            throw new BusinessException("该书已归还，请勿重复操作");
        }
        // 计算逾期
        LocalDateTime now = LocalDateTime.now();
        BigDecimal fine = BigDecimal.ZERO;
        if (now.isAfter(record.getDueTime())) {
            long days = ChronoUnit.DAYS.between(record.getDueTime(), now);
            fine = FINE_PER_DAY.multiply(BigDecimal.valueOf(days));
        }
        record.setReturnTime(now);
        record.setStatus(1);
        record.setFineAmount(fine);
        borrowMapper.updateById(record);

        // 恢复库存
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getBookId, record.getBookId()));
        if (stock != null) {
            stock.setQuantity(stock.getQuantity() + 1);
            stock.setUpdateTime(now);
            stockMapper.updateById(stock);
        }
    }

    /** 续借 */
    public void renew(Long borrowId, Long userId) {
        Borrow record = getMyBorrow(borrowId, userId);
        if (record.getStatus() == 1) {
            throw new BusinessException("该书已归还，无需续借");
        }
        if (record.getRenewCount() >= MAX_RENEW) {
            throw new BusinessException("最多续借 " + MAX_RENEW + " 次");
        }
        record.setDueTime(record.getDueTime().plusDays(RENEW_DAYS));
        record.setRenewCount(record.getRenewCount() + 1);
        record.setStatus(0);
        borrowMapper.updateById(record);
    }

    /** 我的借阅 */
    public List<Map<String, Object>> myBorrows(Long userId) {
        refreshOverdue();
        return borrowMapper.selectMyBorrows(userId);
    }

    // ============ 后台 ============

    public Map<String, Object> adminList(long page, long size) {
        refreshOverdue();
        long offset = (page - 1) * size;
        List<Map<String, Object>> list = borrowMapper.selectAdminList(offset, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("size", size);
        return result;
    }

    public List<Map<String, Object>> overdueList() {
        refreshOverdue();
        return borrowMapper.selectOverdue();
    }

    /** 把已过应还时间但未还的记录标记为逾期 */
    private void refreshOverdue() {
        borrowMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Borrow>()
                        .eq(Borrow::getStatus, 0)
                        .lt(Borrow::getDueTime, LocalDateTime.now())
                        .set(Borrow::getStatus, 2));
    }

    private Borrow getMyBorrow(Long borrowId, Long userId) {
        Borrow record = borrowMapper.selectById(borrowId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (userId != null && !record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人借阅记录");
        }
        return record;
    }
}
