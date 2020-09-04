package com.aigui.gmall.user.controller;

import com.aigui.gmall.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserController {

    @Autowired
    IUserService iUserService;

    @RequestMapping("index")
    @ResponseBody
    public String index(){
        return "hello user";
    }
}
