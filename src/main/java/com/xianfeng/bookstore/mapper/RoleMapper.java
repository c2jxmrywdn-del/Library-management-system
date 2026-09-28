package com.xianfeng.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xianfeng.bookstore.entity.Role;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper
 */
public interface RoleMapper extends BaseMapper<Role> {

    /** 查询某角色拥有的权限 URL 列表（RBAC） */
    @Select("select p.perm_url from t_permission p " +
            "left join t_role_permission rp on p.perm_id = rp.perm_id " +
            "where rp.role_id = #{roleId}")
    List<String> selectPermUrlsByRoleId(Long roleId);
}
