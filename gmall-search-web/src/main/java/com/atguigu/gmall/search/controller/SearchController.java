package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("index")
    public String index(){
        return "index";
    }

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap){
        //调用搜索服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfos=searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList",pmsSearchSkuInfos);

        //抽取检索结果所包含的平台属性的集合
        Set<String>valueIdSet=new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }

        //根据valueId
        List<PmsBaseAttrInfo>pmsBaseAttrInfos=attrService.getAttrValueListByValueId(valueIdSet);
        modelMap.put("attrList",pmsBaseAttrInfos);
        //进一步处理，对平台属性进一步处理，去掉当前条件中valueId所在的属性组






        //modelMap.put("urlParam","catalog3Id=61");
        String urlParam=getUrlParam(pmsSearchParam);
        modelMap.put("urlParam",urlParam);
        return "list";
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam) {
        String urlParams="";
        //关键字  不确定一定有
        String keyword = pmsSearchParam.getKeyword();
        if(StringUtils.isNotBlank(keyword)){
            urlParams=urlParams+"&keyword="+keyword;
        }
        //三级分类id   不确定一定有  但是关键字和三级分类id一定有一个有
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        if(StringUtils.isNotBlank(catalog3Id)){
            urlParams=urlParams+"&catalog3Id="+catalog3Id;
        }
        //分类属性值列表
        String[]valueIds = pmsSearchParam.getValueId();
        if(valueIds!=null) {
            for (String valueId : valueIds) {
                urlParams = urlParams + "&valueId=" + valueId;
            }
        }
        //去掉第一个&
        String urlParamsJ=urlParams.replaceFirst("&","");
        return urlParamsJ;
    }
}
