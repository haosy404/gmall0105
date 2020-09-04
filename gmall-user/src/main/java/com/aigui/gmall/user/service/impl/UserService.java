package com.aigui.gmall.user.service.impl;

import com.aigui.gmall.user.mapper.UserMapper;
import com.aigui.gmall.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService {
    @Autowired
    UserMapper userMapper;
}
