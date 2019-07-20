package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.beans.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;
    @Autowired
    AlipayClient alipayClient;

    @Override
    public void addPayment(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePaymentByOrderSn(PaymentInfo paymentInfo) {

        Example e = new Example(PaymentInfo.class);

        e.createCriteria().andEqualTo("orderSn",paymentInfo.getOrderSn());

        paymentInfoMapper.updateByExampleSelective(paymentInfo,e);
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);// 事务型消息

            Queue payment_success_queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");

            MessageProducer producer = session.createProducer(payment_success_queue);

            MapMessage mapMessage = new ActiveMQMapMessage();

            mapMessage.setString("out_trade_no",paymentInfo.getOrderSn());
            mapMessage.setString("status","success");

            producer.send(mapMessage);

            session.commit();
        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendPaymentStatusCheckQueue(PaymentInfo paymentInfo,int count) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);// 事务型消息

            Queue payment_success_queue = session.createQueue("PAYMENT_CHECK_QUEUE");

            MessageProducer producer = session.createProducer(payment_success_queue);

            MapMessage mapMessage = new ActiveMQMapMessage();

            mapMessage.setString("out_trade_no",paymentInfo.getOrderSn());
            mapMessage.setInt("count",count);

            // 延迟触发
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,30*1000);

            producer.send(mapMessage);

            session.commit();
        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, Object> checkPayment(String out_trade_no) {

        Map<String, Object> returnMap = new HashMap<>();

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderSn(out_trade_no);

        // 调用支付宝接口，检查支付结果
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no", out_trade_no);

        String requestMapJSON = JSON.toJSONString(requestMap);
        request.setBizContent(requestMapJSON);
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("交易已创建,调用成功");
            String tradeStatus = response.getTradeStatus();
            returnMap.put("trade_status",tradeStatus);
            returnMap.put("out_trade_no",out_trade_no);
            returnMap.put("trade_no",response.getTradeNo());
            String responseJSON = JSON.toJSONString(response);
            returnMap.put("callbackContent",responseJSON);
        } else {
            // 如果用户没有登录支付宝，状态是交易未创建
            returnMap.put("trade_status","");
            returnMap.put("out_trade_no",out_trade_no);
            System.out.println("交易未创建，调用失败");
        }

        return returnMap;
    }

    @Override
    public String checkDbPayStatus(String out_trade_no) {

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderSn(out_trade_no);
        PaymentInfo paymentInfo1 = paymentInfoMapper.selectOne(paymentInfo);

        if(StringUtils.isNotBlank(paymentInfo1.getPaymentStatus())&&paymentInfo1.getPaymentStatus().equals("已支付")){
            return "success";
        }else{
            return "fail";
        }

    }
}
