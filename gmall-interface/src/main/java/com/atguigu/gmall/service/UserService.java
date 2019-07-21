package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {

    /**
     * 查询所有用户信息
     *
     * @return
     */
    List<UserInfo> getAll();

    /**
     * 根据用户ID查询用户的地址列表
     *
     * @param userId
     * @return
     */
    List<UserAddress> getUserAddressList(String userId);

    /**
     * 查询用户信息
     *
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 根据用户ID去redis查询用户消息
     *
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
