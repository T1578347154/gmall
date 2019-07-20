package com.atguigu.gmall.service;

import com.atguigu.gmall.beans.OmsCartItem;

import java.util.List;

public interface CartService {
    OmsCartItem isCartExist(OmsCartItem omsCartItem);

    void updateCartById(OmsCartItem omsCartItem1Exist);

    void insertCart(OmsCartItem omsCartItem);

    void updateCartCheck(OmsCartItem omsCartItem);

    List<OmsCartItem> getCartCache(String memberId);
}
