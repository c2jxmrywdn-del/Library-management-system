package com.xianfeng.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xianfeng.bookstore.entity.User;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
public interface UserMapper extends BaseMapper<User> {

    /** 根据用户名查询用户（登录用） */
    @Select("select * from t_user where username = #{username} limit 1")
    User selectByUsername(String username);
}
