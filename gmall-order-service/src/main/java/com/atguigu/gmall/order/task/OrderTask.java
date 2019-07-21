package com.atguigu.gmall.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
public class OrderTask {

    @Reference
    private OrderService orderService;


    @Scheduled(cron = "0/20 * * * * ?")
    public void checkOrder() {

        List<OrderInfo> orderInfoList = orderService.getExpiredOrderList();
        if (orderInfoList != null && orderInfoList.size() > 0) {
            //循环处理过期时间
            for (OrderInfo orderInfo : orderInfoList) {
                //处理单个过期对象
                orderService.execExpiredOrder(orderInfo);
            }
        }

    }

}