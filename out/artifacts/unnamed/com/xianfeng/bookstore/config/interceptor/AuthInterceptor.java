package com.xianfeng.bookstore.config.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xianfeng.bookstore.common.R;
import com.xianfeng.bookstore.entity.User;
import com.xianfeng.bookstore.util.RoleConstants;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;

/**
 * 登录与权限拦截器（RBAC 校验）
 *
 * 路径规则：
 *  - /admin/super/** 仅超级管理员（roleId=4）
 *  - /admin/**      普通管理员与超级管理员（roleId=3,4）
 *  - /cart/**、/order/**、/comment/**、/user/info 需要登录
 *  - 其余前台浏览接口（登录、注册、图书浏览、搜索）游客可访问
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行跨域预检
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        // 去掉 context-path（/api）
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("currentUser");

        // 1. 超级管理员接口
        if (uri.startsWith("/admin/super/")) {
            if (user == null) {
                return reject(response, 401, "请先登录");
            }
            if (!RoleConstants.ADMIN.equals(user.getRoleId())) {
                return reject(response, 403, "无权限：仅超级管理员可操作");
            }
            return true;
        }

        // 2. 后台管理接口
        if (uri.startsWith("/admin/")) {
            if (user == null) {
                return reject(response, 401, "请先登录");
            }
            if (!RoleConstants.STAFF.equals(user.getRoleId()) && !RoleConstants.ADMIN.equals(user.getRoleId())) {
                return reject(response, 403, "无权限：仅管理员可操作");
            }
            return true;
        }

        // 3. 需要登录的前台接口（购物车、订单、借阅、发表评价、个人中心）
        //    注意：/comment/book/** 为游客可查看的公开评价，不在此列
        boolean needLogin = uri.startsWith("/cart/") || uri.startsWith("/order/")
                || uri.startsWith("/borrow/")
                || uri.equals("/comment/add") || uri.equals("/user/info");
        if (needLogin && user == null) {
            return reject(response, 401, "请先登录");
        }
        return true;
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(MAPPER.writeValueAsString(R.fail(code, msg)));
        return false;
    }
}
