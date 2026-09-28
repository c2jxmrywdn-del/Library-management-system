package com.xianfeng.bookstore.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 */
@Data
public class R<T> implements Serializable {

    /** 业务状态码：200成功，401未登录，403无权限，500失败 */
    private Integer code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }

    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
