package com.monkey.monkey.ams.web.controller;

import com.monkey.order.bsm.biz.protocol.OrderProtocol;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/amsBsmControl")
public class AmsBsmController {

    @DubboReference(group = "dev", version = "1.0.0")
    private OrderProtocol protocol;

    /**
     * 这是企业智能助手案例
     *
     * @param request
     * @return
     */
    @PostMapping("/chat")
    public Map<String,String> chat(@RequestBody Map<String, String> request) {

        Map<String,Object> param = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis());
        param.put("orderId",timestamp);
        param.put("shipperUserId","234256578");
        protocol.insertOrder(param);
        return Map.of("reply", "操作成功");

    }

}
