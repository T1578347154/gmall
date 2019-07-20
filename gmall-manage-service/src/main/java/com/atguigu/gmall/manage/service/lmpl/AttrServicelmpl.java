package com.atguigu.gmall.manage.service.lmpl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.beans.PmsBaseAttrInfo;
import com.atguigu.gmall.beans.PmsBaseAttrValue;
import com.atguigu.gmall.beans.PmsBaseCatalog1;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseCatalog1Mapper;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;

@Service
public class AttrServicelmpl implements AttrService {

    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;
    @Autowired
    PmsBaseCatalog1Mapper pmsBaseCatalog1Mapper;

    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String ctatlog3Id) {
        // 属性表 关联 商品
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        // 获取 属性的id
        pmsBaseAttrInfo.setCatalog3Id(ctatlog3Id);
        // 查询pms_Base_Attr_Info表
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        // 遍历
        for (PmsBaseAttrInfo baseAttrInfo : pmsBaseAttrInfos) {
            //获取pms_Base_Attr_Info的 id字段
            baseAttrInfo.setCatalog3Id(ctatlog3Id);
            String attr_id = baseAttrInfo.getId();

            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            // 通过pms_Base_Attr_Info表 主键id 设置到 pms_Base_Attr_Value字段 id
            // pms_Base_Attr_Value 关联 pms_Base_Attr_Info   外键 attr_id
            pmsBaseAttrValue.setAttrId(attr_id);
            // 查询 pms_Base_Attr_Value表
            List<PmsBaseAttrValue> select = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
            // 把 pms_Base_Attr_Value表 的属性 设置到 pms_Base_Attr_Info
            baseAttrInfo.setAttrValueList(select);
        }
        // 返回  表中的详细 字段
        return pmsBaseAttrInfos;
    }


    @Override
    public void saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {
        String id = pmsBaseAttrInfo.getId();
        if (StringUtils.isBlank(id)) {
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
            String attr_id = pmsBaseAttrInfo.getId();
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                pmsBaseAttrValue.setAttrId(attr_id);
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }
        } else {
            pmsBaseAttrInfoMapper.updateByPrimaryKeySelective(pmsBaseAttrInfo);
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(id);
            // 
            pmsBaseAttrValueMapper.delete(pmsBaseAttrValue);

            // 插入
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(id);
                pmsBaseAttrValueMapper.insertSelective(baseAttrValue);
            }

        }
    }



    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
        return pmsBaseAttrValues;
    }

    @Override
    public List<PmsBaseAttrInfo> getAttrValueByValueIds(HashSet<String> valueIdSet) {
        String valueIdsStr = StringUtils.join(valueIdSet, ",");
        List<PmsBaseAttrInfo> pmsBaseAttrInfos=  pmsBaseAttrInfoMapper.selectAttrValueByValueIds(valueIdsStr);
        return pmsBaseAttrInfos;
    }
}
