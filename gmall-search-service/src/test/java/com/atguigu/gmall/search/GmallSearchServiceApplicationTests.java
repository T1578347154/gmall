package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;

import com.atguigu.gmall.beans.*;
import com.atguigu.gmall.service.CcatalogService;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {
     @Reference
    CcatalogService ccatalogService;
     @Autowired
    JestClient jestClient;
     @Reference
    SkuService skuService;

    @Test
    public void contextLoads() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(new File("D:/a.json"));


        PmsBaseCatalog1 pmsBaseCatalog1 = new PmsBaseCatalog1();
        String id1 = pmsBaseCatalog1.getId();
        List<PmsBaseCatalog1> catalog1 = ccatalogService.getCatalog1();
        for (PmsBaseCatalog1 baseCatalog1 : catalog1) {
            List<PmsBaseCatalog2> catalog2 = ccatalogService.getCatalog2(id1);
            for (PmsBaseCatalog2 pmsBaseCatalog2 : catalog2) {
                String id = pmsBaseCatalog2.getId();
                String catalog1Id1 = pmsBaseCatalog2.getCatalog1Id();
                fileOutputStream.write(catalog1Id1.getBytes());
                List<PmsBaseCatalog3> catalog3 = ccatalogService.getCatalog3(id);
                String catalog1Id = pmsBaseCatalog2.getCatalog1Id();
                fileOutputStream.write(catalog1Id.getBytes());
                for (PmsBaseCatalog3 pmsBaseCatalog3 : catalog3) {
                    String s = JSON.toJSONString(pmsBaseCatalog3);
                    fileOutputStream.write(s.getBytes());
                }

            }

        }
    }

    @Test
    public void test3() throws IOException {
        //查询  addIndex对应数据库的 库名 addType 对应 表名
//        Search search = new Search.Builder("{ \"query\": {\"match\": { \"name\": \"杨幂幂\"}}}")
//                .addIndex("gmall0225")
//                .addType("mysql").build();
      List<PmsSkuInfo> pmsSkuInfos= skuService.getAllSku();
      List<PmsSearchSkuInfo> pmsSearchSkuInfoList= new ArrayList();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            // 将 java 的 sku转换 es的 sku
            BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);
            String id = pmsSkuInfo.getId();
            long l = Long.parseLong(id);
            pmsSearchSkuInfo.setId(l);
            pmsSearchSkuInfoList.add(pmsSearchSkuInfo);
        }
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
            Index build = new Index.Builder(pmsSearchSkuInfo).index("gmall0225").type("PmsSearchSkuInfo").id(pmsSearchSkuInfo.getId() + "").build();
            JestResult execute = jestClient.execute(build);
        }


    }



    }


