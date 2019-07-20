package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.beans.UmsMember;
import com.atguigu.gmall.beans.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberMapper;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;


import java.util.List;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UmsMemberMapper umsMemberMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {

        List<UmsMember> allUser = umsMemberMapper.selectAll(); //umsMemberMapper.getAllUser();

        return allUser;
    }

    @Override
    public UmsMember getUserById(String uid) {
        UmsMember umsMember = new UmsMember();
        umsMember.setId(uid);
        UmsMember umsMember1 = umsMemberMapper.selectOne(umsMember);
        return umsMember1;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        UmsMember umsMember1 = new UmsMember();
        umsMember1.setUsername(umsMember.getUsername());
        umsMember1.setPassword(umsMember.getPassword());
        UmsMember umsMemberFromDb = umsMemberMapper.selectOne(umsMember1);
        if (umsMemberFromDb!=null){
            Jedis jedis = redisUtil.getJedis();
            jedis.setex("user:"+umsMemberFromDb.getId()+":info",60*60, JSON.toJSONString(umsMemberFromDb));
            jedis.close();
            return  umsMemberFromDb;
        }else {
            return  null;
        }

    }

    @Override
    public void putTOken(String gmallToken, String memberId) {
        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:"+memberId+":token",60*60,gmallToken);
        jedis.close();
    }

    @Override
    public List<UmsMemberReceiveAddress> getMemberAddressesById(String memberId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
       List<UmsMemberReceiveAddress>umsMemberReceiveAddresses= umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return umsMemberReceiveAddresses;
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
        UmsMember umsMemberReturn = new UmsMember();
        UmsMember umsMember1 = new UmsMember();
        umsMember1.setSourceUid(umsMember.getSourceUid());
        List<UmsMember> select = umsMemberMapper.select(umsMember1);
        if(select==null||select.size()==0){
            umsMemberMapper.insertSelective(umsMember);
            umsMemberReturn = umsMember;
        }else{
            umsMemberReturn = select.get(0);
        }

        return umsMemberReturn;
    }

    @Override
    public UmsMemberReceiveAddress getMemberAddressesByAddressId(String receiveAddressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        UmsMemberReceiveAddress umsMemberReceiveAddress1 = umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);

        return umsMemberReceiveAddress1;
    }
}
