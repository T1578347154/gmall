package com.atguigu.gmall.item.Controll;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.beans.PmsProductSaleAttr;
import com.atguigu.gmall.beans.PmsSkuInfo;
import com.atguigu.gmall.beans.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;


@Controller
public class ItemControll {

    @Reference
    SkuService skuService;
    @Reference
    SpuService spuService;

    @RequestMapping("checkSkuIdByValueIds")
    public String checkSkuIdByValueIds(ModelMap map) {
        String skuId = "";
        skuService.checkSkuByValueIdsTwo(null);
        return skuId;
    }

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap mpe, HttpServletRequest request) {
        // 把查询出来的数据 展示页面上
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId, request.getRemoteAddr());
        mpe.put("skuInfo", pmsSkuInfo);
        // 把在数据库中的 销售属性 展示到页面上
        List<PmsProductSaleAttr> pmsProductSaleAttrs = skuService.spuSaleAttrListCheckedBySkuId(pmsSkuInfo.getProductId(), skuId);
        mpe.put("spuSaleAttrListCheckBySku", pmsProductSaleAttrs);
        //
        List<PmsSkuInfo> pmsSkuInfos = skuService.checkSkuBySpuId(pmsSkuInfo.getProductId());
        HashMap<String, String> skuSaleAttrMap = new HashMap<>();
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String skuIdForHashMap = skuInfo.getId();
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            String valueIds = "";
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                valueIds = valueIds + "|" + pmsSkuSaleAttrValue.getSaleAttrValueId();
            }
            skuSaleAttrMap.put(valueIds, skuIdForHashMap);

        }

        mpe.put("skuSaleAttrMap", JSON.toJSONString(skuSaleAttrMap));
        mpe.put("currentSkuId", skuId);
        return "item";
    }

}
