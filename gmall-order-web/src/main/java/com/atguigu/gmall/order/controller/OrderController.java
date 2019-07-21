package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    @Reference
    private CartService cartService;

    @Reference
    private UserService userService;

    @Reference
    private OrderService orderService;

//    @Reference
//    private ManageService manageService;

    @RequestMapping(value = "/trade")
    @LoginRequire
    public String getUserAddressAll(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<UserAddress> userAddressList = userService.getUserAddressList(userId);

        //显示购物清单 购物车中被选中的商品
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        //创建一个订单明细集合
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        if (cartInfoList != null && cartInfoList.size() > 0) {
            for (CartInfo cartInfo : cartInfoList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                System.out.println(orderDetailList.toString());
                orderDetailList.add(orderDetail);
            }
        }
        //保存总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());
        //获取商品明细列表
        request.setAttribute("orderDetailArrayList", orderDetailList);
        //获取用户地址列表
        request.setAttribute("userAddressList", userAddressList);

        //生成流水号
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo", tradeNo);
        return "trade";
    }

    //下订单
    @RequestMapping(value = "/submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        //未支付
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //计算总金额
        orderInfo.sumTotalAmount();
        //设置用户ID
        orderInfo.setUserId(userId);
        //判断页面流水号是否一致
        String tradeNo = request.getParameter("tradeNo");
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag) {
            //跳转到失败页面
            request.setAttribute("errMsg", "该页面已失效，请重新结算!");
            return "tradeFail";
        }
        //验证商品是否有库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList != null && orderDetailList.size() > 0) {
            for (OrderDetail orderDetail : orderDetailList) {
                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                if (!result) {
                    request.setAttribute("errMsg", orderDetail.getSkuName() + "商品库存不足，请重新下单！");
                    return "tradeFail";
                }
                //验证价格
//                String skuId = orderDetail.getSkuId();
//                SkuInfo skuInfo = manageService.getSkuInfo(skuId);
//                if (orderDetail.getOrderPrice()！=skuInfo.getPrice()) {
//                    request.setAttribute("errMsg", orderDetail.getSkuName() + "价格发生变化，请重新下单！");
//                    return "tradeFail";
//                }
//                cartService.loadCartCache(userId)
            }
        }
        String orderId = orderService.saveOrder(orderInfo);
        //删除缓存中的流水号
        orderService.delTradeCode(userId);
        //重定向到支付模块
        return "redirect://payment.gmall.com/index?orderId=" + orderId;
    }

    //拆单
    @RequestMapping(value = "/orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request) {
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        ArrayList<Map> wareMapList = new ArrayList<>();
        //获取子订单的集合
        List<OrderInfo> subOrderInfoList = orderService.splitOrder(orderId, wareSkuMap);
        for (OrderInfo orderInfo : subOrderInfoList) {
            Map map = orderService.initWareOrder(orderInfo);
            wareMapList.add(map);
        }
        return JSON.toJSONString(wareMapList);
    }

}
