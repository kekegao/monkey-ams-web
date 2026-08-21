package com.monkey.monkey.ams.web.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisTestController {

    /*@Resource
    private RedissonClient redissonClient;

    @GetMapping("/test")
    public String test() {

        RBucket<String> bucket =
                redissonClient.getBucket("test:key");

        bucket.set("hello redis");

        return bucket.get();
    }*/
}
