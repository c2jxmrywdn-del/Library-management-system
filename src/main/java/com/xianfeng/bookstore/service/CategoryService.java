package com.xianfeng.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xianfeng.bookstore.common.BusinessException;
import com.xianfeng.bookstore.entity.Category;
import com.xianfeng.bookstore.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图书分类服务
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /** 分类列表（前台+后台共用，按排序号升序） */
    public List<Category> list() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));
    }

    /** 新增分类 */
    public void add(Category category) {
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        categoryMapper.insert(category);
    }

    /** 修改分类 */
    public void update(Category category) {
        if (categoryMapper.selectById(category.getCategoryId()) == null) {
            throw new BusinessException("分类不存在");
        }
        categoryMapper.updateById(category);
    }

    /** 删除分类 */
    public void delete(Long categoryId) {
        categoryMapper.deleteById(categoryId);
    }
}
