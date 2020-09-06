package com.aigui.gmall.user.service.impl;

import com.aigui.gmall.user.bean.UmsMember;
import com.aigui.gmall.user.bean.UmsMemberReceiveAddress;
import com.aigui.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.aigui.gmall.user.mapper.UserMapper;
import com.aigui.gmall.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements IUserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Override
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMembers= userMapper.selectAll();//userMapper.selectAllUser();
        return umsMembers;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress=new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List< UmsMemberReceiveAddress>UmsMemberReceiveAddres  = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return UmsMemberReceiveAddres;
    }

}
