package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
@CrossOrigin
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    //@LoginRequire//表示用户访问商品详情的时候，前提必须登陆
    @RequestMapping(value = "/{skuId}.html")
    public String skuInfoPage(@PathVariable String skuId, HttpServletRequest request) {

        //System.out.println("商品ID" + skuId);
        //查询商品的信息，销售属性和销售属性值，图片
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        //封装这个商品的销售属性集合
        String key = "";
        HashMap<String, String> map = new HashMap<>();
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpuId(skuInfo.getSpuId());

        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0) {
            for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
                SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
                if (key.length() != 0) {
                    key += "|";
                }
                key += skuSaleAttrValue.getSaleAttrValueId();

                if ((i + 1) == skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i + 1).getSkuId())) {
                    map.put(key, skuSaleAttrValue.getSkuId());
                    key = "";
                }
            }
        }
        String valuesSkuJson = JSON.toJSONString(map);
        System.out.println("valuesSkuJson" + valuesSkuJson);
        request.setAttribute("valuesSkuJson", valuesSkuJson);

        request.setAttribute("spuSaleAttrList", spuSaleAttrList);
        request.setAttribute("skuInfo", skuInfo);

        //调用ListSevice的热度排名
        listService.incrHotScore(skuId);
        return "item";
    }
}
