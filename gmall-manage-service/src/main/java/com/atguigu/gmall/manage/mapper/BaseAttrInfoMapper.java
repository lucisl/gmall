package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    /**
     * 根据三级分类ID查询平台属性和平台属性值
     *
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(String catalog3Id);

    /**
     * 根据平台属性值ID 查询平台属性集合
     *
     * @param strValueId
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("strValueId") String strValueId);
}
