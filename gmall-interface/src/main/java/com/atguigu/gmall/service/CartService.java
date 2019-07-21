package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;

import java.util.List;

public interface CartService {

    /**
     * 添加购物车
     *
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(String skuId, String userId, Integer skuNum);

    /**
     * 根据用户ID查询购物车中的商品信息
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     *
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     * 勾选状态
     *
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据用户ID获取到被勾选的商品列表
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);
}
