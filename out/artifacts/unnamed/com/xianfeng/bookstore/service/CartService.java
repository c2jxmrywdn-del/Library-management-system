package com.xianfeng.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xianfeng.bookstore.common.BusinessException;
import com.xianfeng.bookstore.entity.Book;
import com.xianfeng.bookstore.entity.Cart;
import com.xianfeng.bookstore.entity.Stock;
import com.xianfeng.bookstore.mapper.BookMapper;
import com.xianfeng.bookstore.mapper.CartMapper;
import com.xianfeng.bookstore.mapper.StockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务
 */
@Service
public class CartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private StockMapper stockMapper;

    /** 加入购物车（同书数量累加） */
    public void add(Long userId, Long bookId, Integer quantity) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() == 0) {
            throw new BusinessException("图书不存在或已下架");
        }
        Cart exist = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId).eq(Cart::getBookId, bookId));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            cartMapper.updateById(exist);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setBookId(bookId);
            cart.setQuantity(quantity);
            cart.setAddTime(LocalDateTime.now());
            cartMapper.insert(cart);
        }
    }

    /** 修改数量 */
    public void updateQuantity(Long cartId, Long userId, Integer quantity) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        if (quantity <= 0) {
            throw new BusinessException("数量必须大于0");
        }
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
    }

    /** 删除购物车项 */
    public void delete(Long cartId, Long userId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        cartMapper.deleteById(cartId);
    }

    /** 我的购物车 */
    public List<Map<String, Object>> myCart(Long userId) {
        return cartMapper.selectCartDetail(userId);
    }

    /** 清空某用户购物车 */
    public void clearByUser(Long userId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }

    /** 按购物车项 id 批量查询（下单用） */
    public List<Cart> listByUser(Long userId) {
        return cartMapper.selectList(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }
}
