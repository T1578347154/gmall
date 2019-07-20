package com.atguigu.gmall.service;

import com.atguigu.gmall.beans.PmsSearchParam;
import com.atguigu.gmall.beans.PmsSearchSkuInfo;

import java.util.List;

/**
 * 搜索服务
 */
public interface SearchService {

    List<PmsSearchSkuInfo> search(PmsSearchParam pmsSearchParam);
}
