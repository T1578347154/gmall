package com.atguigu.gmall.service;

import com.atguigu.gmall.beans.PmsProductSaleAttr;
import com.atguigu.gmall.beans.PmsSkuInfo;

import java.util.List;

public interface SkuService {

    void saveSkuInfo(PmsSkuInfo pmsSkuinfo);

    PmsSkuInfo getSkuById(String skuId, String ip);

    List<PmsSkuInfo> checkSkuBySpuId(String productId);

    List<PmsProductSaleAttr> spuSaleAttrListCheckedBySkuId(String productId, String skuId);

    String checkSkuByValueIdsTwo(String[] ids);

    List<PmsSkuInfo> getAllSku();
}
