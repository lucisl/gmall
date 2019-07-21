package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping(value = "/spuImageList")
    public List<SpuImage> getImageList(String spuId) {
        return manageService.getSpuImageList(spuId);
    }

    @RequestMapping(value = "/spuSaleAttrList")
    public List<SpuSaleAttr> getSpuInfoList(String spuId) {
        return manageService.getSpuSaleAttrList(spuId);
    }

    @RequestMapping(value = "/saveSkuInfo")
    public void addSkuInfo(@RequestBody SkuInfo skuInfo) {
        manageService.saveSkuInfo(skuInfo);

    }




}
