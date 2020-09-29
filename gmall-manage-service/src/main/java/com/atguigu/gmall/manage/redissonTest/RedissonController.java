package com.atguigu.gmall.manage.redissonTest;

import com.atguigu.gmall.util.RedisUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class RedissonController {

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    RedissonClient redissonClient;


    public String testRedisson(){

        return null;
    }
}
