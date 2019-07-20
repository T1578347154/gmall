package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.beans.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {



    @Autowired
    UmsMemberMapper umsMemberMapper;

    @Override
    public List<UmsMember> getAllUser() {

        List<UmsMember> allUser = umsMemberMapper.selectAll(); //umsMemberMapper.getAllUser();

        return allUser;
    }

    @Override
    public UmsMember getUserById(String uid) {
        UmsMember umsMember = new UmsMember();
        umsMember.setId(Long.parseLong(uid));
        UmsMember umsMember1 = umsMemberMapper.selectOne(umsMember);
        return umsMember1;
    }
}
