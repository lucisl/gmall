package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@CrossOrigin
public class MangerController {

    @Reference
    private ManageService manageService;

    /**
     * 查询一级分类
     *
     * @return
     */
    @RequestMapping(value = "/getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1List() {
        return manageService.getCatalog1();
    }


    /**
     * 根据一级分类ID查询二级分类信息
     *
     * @param catalog1Id
     * @return
     */
    @RequestMapping(value = "/getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2List(String catalog1Id) {
        return manageService.getCatalog2(catalog1Id);
    }

    /**
     * 根据二级分类ID查询三级分类信息
     *
     * @param catalog2Id
     * @return
     */
    @RequestMapping(value = "/getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3List(String catalog2Id) {
        return manageService.getCatalog3(catalog2Id);
    }

    /**
     * 根据三级分类ID查询平台属性
     *
     * @param catalog3Id
     * @return
     */
    @RequestMapping(value = "/attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(String catalog3Id) {
        return manageService.getAttrList(catalog3Id);
    }


    /**
     * 添加平台信息
     *
     * @param baseAttrInfo
     */
    @RequestMapping(value = "/saveAttrInfo")
    @ResponseBody
    public void addAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
    }

    /**
     * 查询平台属性
     *
     * @param attrId
     * @return
     */
    @RequestMapping(value = "/getAttrValueList", method = RequestMethod.POST)
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        return baseAttrInfo.getAttrValueList();
    }


    @RequestMapping(value = "/spuList")
    @ResponseBody
    public List<SpuInfo> getSpuInfoList(String catalog3Id) {
        return manageService.getSpuInfoList(catalog3Id);

    }


}
