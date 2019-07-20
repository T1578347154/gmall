package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.beans.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Override
    public OmsCartItem isCartExist(OmsCartItem omsCartItem) {

        OmsCartItem omsCartItem1 = new OmsCartItem();

        omsCartItem1.setProductSkuId(omsCartItem.getProductSkuId());
        omsCartItem1.setMemberId(omsCartItem.getMemberId());
        OmsCartItem omsCartItem2 = omsCartItemMapper.selectOne(omsCartItem1);

        return omsCartItem2;
    }

    @Override
    public void updateCartById(OmsCartItem omsCartItemExist) {
        OmsCartItem omsCartItemForUpdate = new OmsCartItem();
        omsCartItemForUpdate.setTotalPrice(omsCartItemExist.getTotalPrice());
        omsCartItemForUpdate.setQuantity(omsCartItemExist.getQuantity());
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id", omsCartItemExist.getId());
        omsCartItemMapper.updateByExampleSelective(omsCartItemForUpdate, example);

        // 同步购物车缓存
        flushCartCache(omsCartItemExist.getMemberId());
    }

    // 同步购物车缓存
    private void flushCartCache(String memberId) {
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();
            String cartCachekey = "user:" + memberId + ":cart";
            OmsCartItem omsCartItem = new OmsCartItem();
            omsCartItem.setMemberId(memberId);
            List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
            Map<String,String> hmmap = new HashMap<>();
            for (OmsCartItem cartItem : omsCartItems) {
                hmmap.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
            }
            jedis.hmset(cartCachekey,hmmap);
        } finally {
            jedis.close();
        }


    }

    @Override
    public void insertCart(OmsCartItem omsCartItem) {
        omsCartItemMapper.insertSelective(omsCartItem);

        // 同步购物车缓存
        flushCartCache(omsCartItem.getMemberId());

    }

    @Override
    public List<OmsCartItem> getCartCache(String memberId) {

        List<OmsCartItem> omsCartItems = new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();

        List<String> hvals = jedis.hvals("user:" + memberId + ":cart");

        if(hvals!=null){
            for (String cartJson : hvals) {
                OmsCartItem omsCartItem = new OmsCartItem();
                omsCartItem = JSON.parseObject(cartJson,OmsCartItem.class);
                omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
                omsCartItems.add(omsCartItem);
            }
        }

        jedis.close();

        return omsCartItems;
    }

    @Override
    public void updateCartCheck(OmsCartItem omsCartItem) {
        OmsCartItem omsCartItemForUpdate = new OmsCartItem();
        omsCartItemForUpdate.setIsChecked(omsCartItem.getIsChecked());
        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("productSkuId",omsCartItem.getProductSkuId()).andEqualTo("memberId",omsCartItem.getMemberId());
        omsCartItemMapper.updateByExampleSelective(omsCartItemForUpdate,e);

        // 同步购物车缓存
        flushCartCache(omsCartItem.getMemberId());

    }
}
