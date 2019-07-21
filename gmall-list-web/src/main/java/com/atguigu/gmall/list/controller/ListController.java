package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@CrossOrigin
@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping(value = "list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request) {
        SkuLsResult search = listService.search(skuLsParams);

        //获取到平台属性值
        List<String> attrValueIdList = search.getAttrValueIdList();
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);
        //System.out.println("baseAttrInfoList" + baseAttrInfoList);

        //制作查询的参数
        String urlParam = makeUrlParam(skuLsParams);
        System.out.println("urlParam" + urlParam);

        //申明面包屑集合：平台属性名称和平台属性值的名称
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();

        //使用迭代器将过滤条件删除重新传给前台页面
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            //获取到平台属性值里的ID
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            //判断地址中的值是否为空
            if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
                //判断后台数据中的值是否为空
                if (attrValueList != null && attrValueList.size() > 0) {
                    for (BaseAttrValue baseAttrValue : attrValueList) {
                        for (String valueIds : skuLsParams.getValueId()) {
                            //判断地址中的是否和几何中的相等，相等则移除
                            if (valueIds.equals(baseAttrValue.getId())) {
                                iterator.remove();

                                //组成一个面包屑集合
                                BaseAttrValue baseAttrValueed = new BaseAttrValue();
                                baseAttrValueed.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());
                                //制作最新的urlParam
                                String newUrlParam = makeUrlParam(skuLsParams, valueIds);
                                baseAttrValueed.setUrlParam(newUrlParam);
                                baseAttrValueArrayList.add(baseAttrValueed);
                            }
                        }
                    }
                }
            }
        }

        //保存一个关键字
        request.setAttribute("keyword", skuLsParams.getKeyword());
        //保存面包屑集合
        request.setAttribute("baseAttrValueArrayList", baseAttrValueArrayList);
        //保存分页信息
        request.setAttribute("totalPages", search.getTotalPages());
        request.setAttribute("pageNo", skuLsParams.getPageNo());

        request.setAttribute("urlParam", urlParam);
        request.setAttribute("baseAttrInfoList", baseAttrInfoList);
        request.setAttribute("skuLsInfoList", search.getSkuLsInfoList());
        return "list";
    }


    /**
     * 制作查询参数的方法
     *
     * @param skuLsParams     url地址 后面的参数条件
     * @param excludeValueIds 用户点击面包屑的时候，获取的valueId
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams, String... excludeValueIds) {
        String urlParam = "";
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {
            urlParam += "keyword=" + skuLsParams.getKeyword();
        }
        //拼接三级分类ID
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            if (urlParam.length() > 0) {
                urlParam += "&";
            }
            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }
        //拼接属性值ID
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            for (String valueId : skuLsParams.getValueId()) {
                //判断两个平台属性值ID相同{用户点击面包屑的属性值ID和url地址中的属性值ID比较}
                if (excludeValueIds != null && excludeValueIds.length > 0) {
                    //用户点击面包屑获取到的销售属性值id
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)) {
                        continue;
                    }
                }
                if (urlParam.length() > 0) {
                    urlParam += "&";
                }
                urlParam += "valueId=" + valueId;
            }
        }

        return urlParam;
    }
}
