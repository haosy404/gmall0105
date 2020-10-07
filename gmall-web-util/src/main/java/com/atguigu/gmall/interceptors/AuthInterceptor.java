package com.atguigu.gmall.interceptors;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截代码
        //判断被拦截的请求的访问的方法的注解(是否需要拦截的)
        HandlerMethod hm=(HandlerMethod)handler;
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);
        //是否拦截
        if(methodAnnotation==null){
            //不进行拦截
            return true;
        }
        //进行拦截，使用token来进行认证登录处理
        String token="";
        //如果之前登录过，取之前登录后存取的token
        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        if(StringUtils.isNotBlank(oldToken)){
            token=oldToken;
        }

        //获取当前页面最新的token
        String newToken = request.getParameter("token");
        if(StringUtils.isNotBlank(newToken)){
            token=newToken;
        }

        //就算old/new token都存在，那么后面的新token也会覆盖之前的old，逻辑上也是成立的
        //通过上面的2个if，模拟了4种情况
        /**
         *               老token空    老token不空
         *
         * 新token空     从未登录过    之前登陆过
         *
         * 新token不空   刚刚登录      过期/使用新的token
         *
         */

        //在必须拦截的情况下，判断是否需要必须登录
        boolean loginSuccess = methodAnnotation.loginSuccess();

        //调用认证中心进行验证，看是否登录成功
        String success="fail";
        Map<String,String>successMap=new HashMap<>();
        if(StringUtils.isNotBlank(token)){
            //通过nginx转发的客户端ip
            String ip = request.getHeader("x-forwarded-for");
            //如果没有通过nginx转发，就读取原生的
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();
                //如果这个ip也没有，就赋值默认值
                if(StringUtils.isBlank(ip)){
                    ip="127.0.0.1";
                }
            }
            String successJson=HttpclientUtil.doGet("http://127.0.0.1:8085/verify?token="+token+"&currentIp"+ip);
            successMap= JSON.parseObject(successJson,Map.class);
            success=successMap.get("status");
        }
        if(loginSuccess){
            //必须登录

            //必须登录成功才能使用
            //如果登录失败，重定向踢回重新登录
            if(!success.equals("success")){
                //获取当请求过来的地址
                StringBuffer requestURL = request.getRequestURL();
                //重定向
                response.sendRedirect("http://127.0.0.1:8085/index?ReturnUrl="+requestURL);
            }
            //如果登录成功
            //将token携带的用户信息写入
            //下面为测试数据，后续优化为动态数据
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname", successMap.get("nickname"));
            //登录成功验证通过，同时处理覆盖之前cookie中的token，进行更新
            if(StringUtils.isNotBlank(token)){
                CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
            }

        }else{
            //不是必须登录，但是必须拦截一下的
            if (success.equals("success")) {
                 // 需要将token携带的用户信息写入
                 request.setAttribute("memberId", successMap.get("memberId"));
                 request.setAttribute("nickname", successMap.get("nickname"));
                 //验证通过，覆盖cookie中的token
                 if(StringUtils.isNotBlank(token)){
                     CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                 }
            }
        }

        return true;
    }
}
