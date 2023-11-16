package com.wokoworks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 测试 启动类
 */
@SpringBootTest(classes = {TronApplication.class})
public class TestApplication {



    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
