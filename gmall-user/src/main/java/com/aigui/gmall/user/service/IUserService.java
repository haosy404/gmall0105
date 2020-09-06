package com.aigui.gmall.user.service;

import com.aigui.gmall.user.bean.UmsMember;
import com.aigui.gmall.user.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface IUserService {
    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);
}
