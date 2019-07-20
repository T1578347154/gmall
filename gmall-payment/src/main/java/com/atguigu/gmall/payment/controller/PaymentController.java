package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.beans.OmsOrder;
import com.atguigu.gmall.beans.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {
    @Autowired
    AlipayClient alipayClient;
    @Reference
    OrderService orderService;

    @Autowired
    PaymentService paymentService;


    @RequestMapping("alipay/callback/return")
    @ResponseBody
    public String callback(HttpServletRequest request, String orderSn, BigDecimal totalAmount, ModelMap map) {

        // 验证签名
        String sign = request.getParameter("sign");

        String out_trade_no = request.getParameter("out_trade_no");
        // 先检查幂等性
        String payStatus = paymentService.checkDbPayStatus(out_trade_no);

        if(!payStatus.equals("success")){
            // 回调时间
            // 回调内容
            // 支付宝交易单号
            String trade_no = request.getParameter("trade_no");
            String app_id = request.getParameter("app_id");

            String queryString = request.getQueryString();



            // 更新支付信息
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);
            paymentInfo.setCallbackContent(queryString);
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setOrderSn(out_trade_no);

            // 调用订单系统，进一步更新订单信息，发送消息队列，支付成功PAYMENT_SUCCESS_QUEUE
            paymentService.sendPaymentResult(paymentInfo);

            paymentService.updatePaymentByOrderSn(paymentInfo);


            //  锁定库存
            // wareService.updateWare()
            //  调用物流
            // ...
        }

        return "redirect:/paySuccess.html";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipay(String orderSn, BigDecimal totalAmount, ModelMap map) {

        // 根据支付宝的客户端生成一个重定向地址，让客户重定向到支付宝支付页面
        OmsOrder omsOrder = orderService.getOrderByOrderSn(orderSn);

        // 封装公共参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        // 封装业务参数
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no", orderSn);
        requestMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        requestMap.put("total_amount", "0.01");
        requestMap.put("subject", omsOrder.getOmsOrderItems().get(0).getProductName());
        String requestMapJSON = JSON.toJSONString(requestMap);
        alipayRequest.setBizContent(requestMapJSON);//填充业务参数

        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        System.out.println(form);

        // 生成支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(orderSn);
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setSubject(omsOrder.getOmsOrderItems().get(0).getProductName());
        paymentInfo.setTotalAmount(totalAmount);
        paymentService.addPayment(paymentInfo);

        // 发送一个延迟检查的队列，检查当前订单支付状态
        paymentService.sendPaymentStatusCheckQueue(paymentInfo,5);// 设置检查5次
        return form;
    }

    @RequestMapping("mx/submit")
    public String mx(String orderSn, BigDecimal totalAmount, ModelMap map) {

        return null;
    }

    @RequestMapping("index")
    public String index(String orderSn, BigDecimal totalAmount, ModelMap modelMap){
         modelMap.put("orderSn",orderSn);
         modelMap.put("totalAmount",totalAmount);
        return "index";
    }
}
