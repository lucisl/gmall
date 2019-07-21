package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {

    /**
     * 将skuLsInfo保存到ES中
     *
     * @param skuLsInfo
     */
    void saveSkuInfo(SkuLsInfo skuLsInfo);

    /**
     * 根据条件查询数据返回结果==全文检索
     *
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 记录商品关注量，做热度排名
     *
     * @param skuId
     */
    void incrHotScore(String skuId);
}
