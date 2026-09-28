package com.xianfeng.bookstore;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 先锋书店管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.xianfeng.bookstore.mapper")
public class XianfengBookstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(XianfengBookstoreApplication.class, args);
        System.out.println("===============================================");
        System.out.println("  先锋书店管理系统启动成功！");
        System.out.println("  接口前缀: http://localhost:8080/api");
        System.out.println("===============================================");
    }
}
