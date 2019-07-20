package com.atguigu.gmall.order.Controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.beans.*;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    UserService userService;
    @Reference
    CartService cartService;
    @Reference
    OrderService orderService;
    @Reference
    SkuService skuService;

    @LoginRequired(isNeededSuccess = true)
    @RequestMapping("submitOrder")
    public String submitOrder(String receiveAddressId,HttpServletRequest request, String tradeCode,ModelMap modelMap){
        // 获取用户id，根据用户id查询购物车列表
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");
        // 生成订单时要保证订单不能重复提交
        boolean b = orderService.checkTradeCode(memberId,tradeCode);

        if(b==true){

            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getMemberAddressesByAddressId(receiveAddressId);

            // 根据购物车列和用户选择的收获地址表生成订单
            List<OmsCartItem> cartItems = cartService.getCartCache(memberId);

            // 将订单保存数据库
            // 生成订单数据时验价，验库存
            OmsOrder omsOrder = new OmsOrder();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = sdf.format(new Date());
            long l = System.currentTimeMillis();
            long l1 = System.nanoTime();
            String orderSn = "gmall"+format+l+"";
            omsOrder.setOrderSn(orderSn);
            omsOrder.setCreateTime(new Date());
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setOrderType(0);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            omsOrder.setStatus("0");
            omsOrder.setSourceType(0);
            omsOrder.setTotalAmount(getCartSumPrice(cartItems));


            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            for (OmsCartItem cartItem : cartItems) {
                if(cartItem.getIsChecked().equals("1")){
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    // 将购物车对象转化为订单对象
                    BigDecimal currentPrice = cartItem.getPrice();
                    // 验价
                    String productSkuId = cartItem.getProductSkuId();
                    PmsSkuInfo skuInfo = skuService.getSkuById(productSkuId, "");
                    int i = currentPrice.compareTo(skuInfo.getPrice());
                    if(i==0){
                        omsOrderItem.setProductPrice(cartItem.getPrice());
                    }else{
                        // 价格已经发生变化，处理异常
                        modelMap.put("err","价格发生变化");
                        return "tradeFail";
                    }
                    // 验库存
                    omsOrderItem.setProductQuantity(cartItem.getQuantity());
                    omsOrderItem.setRealAmount(omsOrderItem.getProductPrice().multiply(omsOrderItem.getProductQuantity()));
                    omsOrderItem.setProductSkuId(cartItem.getProductSkuId());
                    omsOrderItem.setProductPic(cartItem.getProductPic());
                    omsOrderItem.setProductName(cartItem.getProductName());
                    omsOrderItem.setProductId(cartItem.getProductId());
                    omsOrderItem.setProductCategoryId(cartItem.getProductCategoryId());

                    omsOrderItem.setOrderSn(orderSn);// 外部订单号


                    omsOrderItems.add(omsOrderItem);
                }
            }

            omsOrder.setOmsOrderItems(omsOrderItems);

            orderService.addOrder(omsOrder);

            // 删除购物车数据

            // 重定向到支付页面
            return "redirect:http://payment.gmall.com:8090/index.html?orderSn="+orderSn+"&totalAmount="+getCartSumPrice(cartItems);// 重定向到支付页面

        }else{
            modelMap.put("err","不能同时提交多个订单");
            return "tradeFail";
        }


    }

    @LoginRequired(isNeededSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, ModelMap modelMap){
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");

        // 根据用户id查询出用户在购物车中选择的商品
        List<OmsCartItem> omsCartItems = cartService.getCartCache(memberId);

        // 将商品对象转化成临时的订单对象
        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        for (OmsCartItem omsCartItem : omsCartItems) {
            if(omsCartItem.getIsChecked().equals("1")){
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                omsOrderItem.setProductId(omsCartItem.getProductId());
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                omsOrderItem.setRealAmount(omsOrderItem.getProductPrice().multiply(omsOrderItem.getProductQuantity()));

                omsOrderItems.add(omsOrderItem);
            }
        }

        // 查询出用户的支付方式和收获地址的列表
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = userService.getMemberAddressesById(memberId);

        modelMap.put("userAddressList",umsMemberReceiveAddresses);
        modelMap.put("orderDetailList",omsOrderItems);
        modelMap.put("totalAmount",getCartSumPrice(omsCartItems));

        // 生成交易码，并且存储在页面上一份
        String tradeCode = orderService.genTradeCode(memberId);

        modelMap.put("tradeCode",tradeCode);
        return "trade";
    }

    private BigDecimal getCartSumPrice(List<OmsCartItem> omsCartItems) {

        BigDecimal sum = new BigDecimal("0");

        for (OmsCartItem omsCartItem : omsCartItems) {
            String isChecked = omsCartItem.getIsChecked();
            if(isChecked.equals("1")){
                sum = sum.add(omsCartItem.getTotalPrice());
            }
        }

        return sum;
    }
}
