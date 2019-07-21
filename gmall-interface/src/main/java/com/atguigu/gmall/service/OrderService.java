package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    /**
     * 下订单
     *
     * @param orderInfo
     * @return 订单编号
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成流水号
     *
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 验证流水号
     *
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    boolean checkTradeCode(String userId, String tradeCodeNo);

    /**
     * 删除流水号
     *
     * @param userId
     */
    void delTradeCode(String userId);

    /**
     * 判断当前是否有库存
     *
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 根据订单ID查询订单信息
     *
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 清空购物车
     *
     * @param userId
     */
    void clean(String userId);

    /**
     * 消费者接受消息，更新订单状态
     *
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(String orderId, ProcessStatus processStatus);

    /**
     * 发送消息给库存
     *
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 获取过期订单
     *
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期订单
     *
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /**
     * 将orderInfo转换为map
     *
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单
     *
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);
}
