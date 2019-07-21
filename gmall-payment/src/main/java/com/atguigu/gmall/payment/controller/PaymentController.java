package com.atguigu.gmall.payment.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;


    //前往支付页面
    @RequestMapping(value = "/index")
    @LoginRequire
    public String index(String orderId, HttpServletRequest request) {
        request.setAttribute("orderId", orderId);
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());
        return "index";
    }

    //交易记录保存
    @RequestMapping(value = "/alipay/submit")
    @LoginRequire
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response) {
        //获取订单ID
        String orderId = request.getParameter("orderId");
        //获取订单对象
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderId);
        paymentInfo.setSubject("--给丫头的--");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        //保存交易信息
        paymentService.savePaymentInfo(paymentInfo);

        // 支付宝参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        //异步回调路径
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        // 声明一个Map
        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", paymentInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", paymentInfo.getTotalAmount());
        map.put("subject", paymentInfo.getSubject());
        // 将map变成json
        String Json = JSON.toJSONString(map);
        alipayRequest.setBizContent(Json);

        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");

        //发送消息 代码追后面 15秒执行一次，总共需要执行3次。
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(), 15, 3);
        return form;
    }


    //同步回调
    @RequestMapping(value = "/alipay/callback/return")
    @LoginRequire
    public String callbackReturn(HttpServletRequest request) {
        //重定向到订单列表
        //清空购物车
        String userId = (String) request.getAttribute("userId");
        //orderService.clean(userId);
        return "redirect:" + AlipayConfig.return_order_url;
    }

    //异步回调
    @RequestMapping(value = "/alipay/callback/notify")
    @ResponseBody
    @LoginRequire
    public String paymentNotify(@RequestParam Map<String, String> paramMap, HttpServletRequest request) {
        boolean flag = false;
        try {
            flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);
            if (flag) {
                //验签成功，按照支付结果，异步通知 二次校验
                String trade_status = paramMap.get("trade_status");
                if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                    String out_trade_no = paramMap.get("out_trade_no");
                    PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no);
                    //当前订单被交易完成
                    if (paymentInfo.getPaymentStatus() == PaymentStatus.PAID || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED) {
                        return "failure";
                    }
                    //记录交易成功，修改支付状态
                    PaymentInfo paymentInfoUpd = new PaymentInfo();
                    // 设置状态
                    paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                    // 设置回调时间
                    paymentInfoUpd.setCallbackTime(new Date());
                    // 设置内容
                    paymentInfoUpd.setCallbackContent(paramMap.toString());
                    paymentService.updatePaymentInfo(out_trade_no, paymentInfoUpd);
                    //消息队列新订单状态
                    paymentService.sendPaymentResult(paymentInfo, "success");
                    return "success";
                }
            } else {
                //验签失败则记录日常日志
                return "failure";
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return "failure";
    }

    // 发送验证
    @RequestMapping(value = "/sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo, String result) {
        paymentService.sendPaymentResult(paymentInfo, result);
        return "sendPaymentResult";
    }

    //查询支付是否成功
    @RequestMapping(value = "/queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request) {
        String orderId = request.getParameter("orderId");
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        boolean flag = paymentService.checkPayment(paymentInfo);
        return "" + flag;
    }
}
