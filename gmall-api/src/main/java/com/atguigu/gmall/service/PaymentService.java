package com.atguigu.gmall.service;

import com.atguigu.gmall.beans.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void addPayment(PaymentInfo paymentInfo);


    void updatePaymentByOrderSn(PaymentInfo paymentInfo);

    void sendPaymentResult(PaymentInfo paymentInfo);

    Map<String,Object> checkPayment(String out_trade_no);

    void sendPaymentStatusCheckQueue(PaymentInfo paymentInfo, int count);

    String checkDbPayStatus(String out_trade_no);
}
