package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.handler.CartCookieHandler;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @LoginRequire(autoRedirect = false)
    @RequestMapping(value = "/addToCart")
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {
        //判断当前是否登陆
        String userId = (String) request.getAttribute("userId");
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");
        if (userId != null) {
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));
        } else {
            //用户未登录下添加购物车，放入cookie
            System.out.println("skuNum:" + skuNum);
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));
        }
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);
        return "success";
    }

    @RequestMapping(value = "/cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (userId != null) {
            //当前是登陆状态
            //1.查看未登录状态下购物车是否存在
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            if (cartListCK != null && cartListCK.size() > 0) {
                //合并购物车
                cartInfoList = cartService.mergeToCartList(cartListCK, userId);
                //2.合并之后将未登录之后的购物车删除
                cartCookieHandler.deleteCartCookie(request, response);
            } else {
                //不需要合并，从redis中取得，或者从数据库中
                cartInfoList = cartService.getCartList(userId);
            }

        } else {
            //查询未登录之后的购物车列表
            cartInfoList = cartCookieHandler.getCartList(request);
        }
        //保存购物车列表，用于页面展示
        request.setAttribute("cartInfoList", cartInfoList);
        return "cartList";
    }

    //购物车是否选中商品
    @RequestMapping(value = "/checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request, HttpServletResponse response) {
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        String userId = (String) request.getAttribute("userId");
        //判断当前是否登陆状态
        if (userId != null) {
            //登陆状态
            cartService.checkCart(skuId, isChecked, userId);
        } else {
            //未登录状态
            cartCookieHandler.checkCart(request, response, skuId, isChecked);
        }

    }

    /**
     * 点击去结算，重定向到订单页面
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");
        //获取未登录购物车数据
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
        if (cartListCK != null && cartListCK.size() > 0) {
            //开始合并
            cartService.mergeToCartList(cartListCK, userId);
            //删除cookie购物车
            cartCookieHandler.deleteCartCookie(request, response);
        }

        return "redirect://order.gmall.com/trade";
    }
}
