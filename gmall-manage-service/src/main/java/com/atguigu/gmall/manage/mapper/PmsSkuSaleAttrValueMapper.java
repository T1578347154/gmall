package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.beans.PmsProductSaleAttr;
import com.atguigu.gmall.beans.PmsSkuInfo;
import com.atguigu.gmall.beans.PmsSkuSaleAttrValue;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface PmsSkuSaleAttrValueMapper extends Mapper<PmsSkuSaleAttrValue> {
    List<PmsSkuInfo> selectCheckSkuBySpuId(@Param("spuId") String spuId);

    List<PmsProductSaleAttr> selectSpuSaleAttrListCheckedBySkuId(@Param("spuId") String spuId, @Param("skuId") String skuId);

    List<PmsSkuInfo> selectCheckSkuByValueIdsTwo(@Param("join") String join);

}
