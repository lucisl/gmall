package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PayMentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;

@Service
public class PayMentServiceImpl implements PaymentService {

    @Autowired
    private PayMentInfoMapper payMentInfoMapper;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Autowired
    private AlipayClient alipayClient;

    //保存交易记录
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        payMentInfoMapper.insertSelective(paymentInfo);

    }

    //根据第三方交易编号查询paymentInfo对象
    @Override
    public PaymentInfo getPaymentInfo(String out_trade_no) {
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(out_trade_no);
        return payMentInfoMapper.selectOne(paymentInfo);
    }

    //更新交易状态
    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo", out_trade_no);
        payMentInfoMapper.updateByExampleSelective(paymentInfoUpd, example);
    }

    //发送消息给订单模块
    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        Connection connection = activeMQUtil.getConnection();
        try {
            //打开连接
            connection.start();
            //创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            //创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_queue);
            //创建消息
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId", paymentInfo.getOrderId());
            activeMQMapMessage.setString("result", result);
            producer.send(activeMQMapMessage);
            //提交消息
            session.commit();
            //关闭
            producer.close();
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    //查询支付是否成功
    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // 将第三方交易编号传入查询接口
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", paymentInfoQuery.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 表示改订单在支付宝中存在！
        if (response.isSuccess()) {
            // trade_status TRADE_SUCCESS TRADE_FINISHED
            if ("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus())) {
                System.out.println("调用成功");
                // 修改交易记录的状态！
                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                updatePaymentInfo(paymentInfoQuery.getOutTradeNo(), paymentInfoUPD);
                // 发送支付成功的消息给订单！
                sendPaymentResult(paymentInfoQuery, "success");
                return true;
            }
        } else {
            System.out.println("调用失败");
        }
        return false;
    }

    //反复调用延迟队列
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {
        //获取连接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            //创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            //创建消息提供者
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            //创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo", outTradeNo);
            activeMQMapMessage.setInt("delaySec", delaySec);
            activeMQMapMessage.setInt("checkCount", checkCount);
            //设置延迟多长时间
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delaySec * 1000);

            producer.send(activeMQMapMessage);
            session.commit();
            //关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    //修改交易记录表 关闭支付信息
    @Override
    public void closePayment(String orderId) {
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId", orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        payMentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }
}
