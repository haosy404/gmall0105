package com.atguigu.gmall.passport.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PassPortController {
    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap modelMap){
        if(StringUtils.isNotBlank(ReturnUrl)) {
            modelMap.put("ReturnUrl", ReturnUrl);
        }
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(){
        //调用用户服务验证用户名和密码
        return "token";
    }
}
