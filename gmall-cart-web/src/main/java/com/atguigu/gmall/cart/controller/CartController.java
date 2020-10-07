package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.support.TaskUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
//cookie工具类可能有点问题，后续修改
@Controller
public class CartController {

    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;

    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap) {
        String memberId=(String) request.getAttribute("memberId");
        String nickname=(String) request.getAttribute("nickname");
        return "toTrade";
    }

    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response){
        //quantity商品数量
        List<OmsCartItem>omsCartItems=new ArrayList<>();

        //调用商品服务查询商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId);

        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("11111111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));

        //判断用户是否登录
        //String memberId="1";//"1"
        String memberId=(String) request.getAttribute("memberId");
        String nickname=(String) request.getAttribute("nickname");
        if(StringUtils.isBlank(memberId)){
            //调用cookie  //如果没登录

            //cookie里原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isBlank(cartListCookie)){
                //cookie为空
                omsCartItems.add(omsCartItem);
            }else{
                //cookie不为空
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                //判断添加的购物车数据在cookie中是否存在
                boolean exist=if_cart_exist(omsCartItems, omsCartItem);
                if(exist){
                    //之前添加过，更新购物车添加数量
                    for (OmsCartItem cartItem : omsCartItems) {
                        if(cartItem.getProductSkuId().equals(omsCartItem.getProductId())){
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                        }
                    }
                }else {
                    //之前没有添加，新增到当前的购物车
                    omsCartItems.add(omsCartItem);
                }
            }

            //更新cookie
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);
        }else {
            //用户登录，采用DB+redis
            OmsCartItem omsCartItemFormDb=cartService.ifCartExistByUser(memberId,skuId);

            if(omsCartItemFormDb==null){
                //该用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname("test小郝");
                omsCartItem.setQuantity(new BigDecimal(quantity));
                //添加DB
                cartService.addCart(omsCartItem);
            }else{
                //用户添加过当前商品
                omsCartItemFormDb.setQuantity(omsCartItemFormDb.getQuantity().add(omsCartItem.getQuantity()));
                //修改DB
                cartService.updateCart(omsCartItemFormDb);
            }

            //同步缓存
            cartService.flushCartCache(memberId);
        }
        //返回重定向,默认不写是转发
        return "redirect:/success.html";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        boolean b=false;
        for (OmsCartItem cartItem : omsCartItems) {
            String productSkuId = cartItem.getProductSkuId();
            if(productSkuId.equals(omsCartItem.getProductSkuId())){
                b=true;
            }
        }
        return b;
    }

    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        //String memberId="1";
        String memberId=(String) request.getAttribute("memberId");
        String nickname=(String) request.getAttribute("nickname");
        if(StringUtils.isNotBlank(memberId)){
            //登录，查询db
            omsCartItems=cartService.cartList(memberId);
        }else {
            //未登录，查询cookie
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if(StringUtils.isNotBlank(cartListCookie)){
                omsCartItems = JSON.parseArray(cartListCookie,OmsCartItem.class);
            }
        }

        for (OmsCartItem omsCartItem : omsCartItems) {
            //手动计算一下总价格，商品价格*商品件数
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
        }
        modelMap.put("cartList",omsCartItems);
        //被勾选商品的总额
        BigDecimal totalAmount=getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        return "cartList";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal bigDecimal=omsCartItem.getTotalPrice();
            if(omsCartItem.getIsChecked().equals("1")){
                totalAmount=totalAmount.add(bigDecimal);
            }
        }
        return totalAmount;
    }

    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isChecked,String skuId,HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap) {
        //String memberId="1";
        String memberId=(String) request.getAttribute("memberId");
        String nickname=(String) request.getAttribute("nickname");
        //调用服务，修改状态
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isChecked);
        //调用服务
        cartService.checkCart(omsCartItem);

        //将最新的数据从缓存中取出，渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        modelMap.put("cartList",omsCartItems);
        //被勾选商品的总额
        BigDecimal totalAmount=getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        return "cartListInner";
    }


}
