package com.xianfeng.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xianfeng.bookstore.common.BusinessException;
import com.xianfeng.bookstore.entity.User;
import com.xianfeng.bookstore.mapper.UserMapper;
import com.xianfeng.bookstore.util.RoleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务：注册、登录、个人资料、读者管理
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /** 注册（默认注册为读者角色） */
    public void register(String username, String password, String realName, String phone) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BusinessException("用户名和密码不能为空");
        }
        User exist = userMapper.selectByUsername(username);
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRoleId(RoleConstants.READER);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    /** 登录，返回登录用户（脱敏） */
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被冻结，请联系管理员");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        // 脱敏
        user.setPassword(null);
        return user;
    }

    /** 更新个人资料 */
    public void updateProfile(Long userId, String realName, String phone, String email, String address) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setRealName(realName);
        user.setPhone(phone);
        user.setEmail(email);
        user.setAddress(address);
        userMapper.updateById(user);
    }

    /** 后台：分页查询读者 */
    public Map<String, Object> pageReaders(long page, long size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), User::getUsername, keyword)
                .orderByDesc(User::getCreateTime);
        Page<User> p = userMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("total", p.getTotal());
        result.put("list", p.getRecords());
        return result;
    }

    /** 后台：冻结/解冻账号 */
    public void changeStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (RoleConstants.ADMIN.equals(user.getRoleId())) {
            throw new BusinessException("不能操作超级管理员账号");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }
}
