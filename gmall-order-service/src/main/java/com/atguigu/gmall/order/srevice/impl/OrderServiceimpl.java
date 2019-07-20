package com.atguigu.gmall.order.srevice.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.beans.OmsOrder;
import com.atguigu.gmall.beans.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Service
public class OrderServiceimpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public void addOrder(OmsOrder omsOrder) {

        // 添加订单，生成订单号
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        // 根据订单号，添加订单详情
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();

        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }


    }

    @Override
    public String genTradeCode(String memberId) {

        Jedis jedis = redisUtil.getJedis();
        String tradeCode = UUID.randomUUID().toString();
        jedis.setex("user:"+memberId+":tradeCode",60*30,tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public boolean checkTradeCode(String memberId,String tradeCode) {

        boolean b = false;

        Jedis jedis = redisUtil.getJedis();

        //对比防重删令牌
//        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//
//        Long eval = (Long) jedis.eval(script, Collections.singletonList("user:"+memberId+":tradeCode"),
//                Collections.singletonList(tradeCode));
//
//        if(!eval.equals("0")){
//            b = true;
//        }

        String tradeCodeFromCache = jedis.get("user:"+memberId+":tradeCode");

        if(StringUtils.isNotBlank(tradeCodeFromCache)&&tradeCodeFromCache.equals(tradeCode)){
            b = true;
            jedis.del("user:"+memberId+":tradeCode");
        }

        jedis.close();

        return b;
    }

    @Override
    public OmsOrder getOrderByOrderSn(String orderSn) {

        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(orderSn);

        OmsOrder omsOrder1 = omsOrderMapper.selectOne(omsOrder);

        OmsOrderItem omsOrderItem = new OmsOrderItem();
        omsOrderItem.setOrderId(omsOrder1.getId());
        List<OmsOrderItem> select = omsOrderItemMapper.select(omsOrderItem);
        omsOrder1.setOmsOrderItems(select);
        return omsOrder1;
    }

    @Override
    public void updateOrder(OmsOrder omsOrder) {
        // 添加订单，生成订单号
        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();
        // 根据订单号，添加订单详情
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();

        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
    }

    @Override
    public void sendOrderPayQueue(String out_trade_no) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);// 事务型消息

            Queue payment_success_queue = session.createQueue("ORDER_PAY_QUEUE");

            MessageProducer producer = session.createProducer(payment_success_queue);

            TextMessage textMessage = new ActiveMQTextMessage();

            // 通知库存系统进行库存锁定
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setOrderSn(out_trade_no);
            omsOrder = omsOrderMapper.selectOne(omsOrder);

            OmsOrderItem omsOrderItem = new OmsOrderItem();
            omsOrderItem.setOrderId(omsOrder.getId());
            omsOrderItem.setOrderSn(omsOrder.getOrderSn());

            List<OmsOrderItem> omsOrderItems = omsOrderItemMapper.select(omsOrderItem);

            omsOrder.setOmsOrderItems(omsOrderItems);

            textMessage.setText(JSON.toJSONString(omsOrder));

            producer.send(textMessage);

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
    }

