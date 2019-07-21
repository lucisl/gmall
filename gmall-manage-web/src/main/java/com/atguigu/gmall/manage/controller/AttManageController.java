package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class AttManageController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;


    @RequestMapping(value = "/onSale")
    public void onSale(String skuId) {

        SkuLsInfo skuLsInfo = new SkuLsInfo();

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        skuLsInfo.setId(skuInfo.getId());
        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        listService.saveSkuInfo(skuLsInfo);

    }


}
