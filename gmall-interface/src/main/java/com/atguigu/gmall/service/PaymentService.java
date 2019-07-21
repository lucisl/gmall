package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {

    /**
     * 保存交易记录
     *
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 根据第三方交易编号查询patmentInfo对象
     *
     * @param out_trade_no
     * @return
     */
    PaymentInfo getPaymentInfo(String out_trade_no);

    /**
     * 更新状态
     *
     * @param out_trade_no
     * @param paymentInfoUpd
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    /**
     * 发送消息给订单模块
     *
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo, String result);

    /**
     * 查询支付是否成功
     *
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     * 延迟队列反复调用
     *
     * @param outTradeNo 单号
     * @param delaySec   延迟时间
     * @param checkCount 次数
     */
    void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount);

    /**
     * 修改交易记录表
     *
     * @param orderId
     */
    void closePayment(String orderId);
}
