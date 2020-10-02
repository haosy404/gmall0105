package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("index")
    public String index() {
        return "index";
    }

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {
        //调用搜索服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", pmsSearchSkuInfos);

        //抽取检索结果所包含的平台属性的集合
        Set<String> valueIdSet = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }

        //根据valueId
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueId(valueIdSet);
        modelMap.put("attrList", pmsBaseAttrInfos);
        //进一步处理，对平台属性进一步处理，去掉当前条件中valueId所在的属性组
        String[] delValueIds = pmsSearchParam.getValueId();
        if (delValueIds != null) {
            //面包屑  valueId   valueName  urlParam
            //面包屑代码优化改造
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            //迭代器，避免初始办法的空指针异常/避免下标越界
            //Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
            for (String delValueId : delValueIds) {  //代码改造优化
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();  //bug 迭代器应该放在第一层循环中
                //放在里面保证每一次循环完成，重新扫一次迭代器，避免bug
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                //经过数据处理,生成面包屑的参数 valueId   valueName  urlParam
                //valueId
                pmsSearchCrumb.setValueId(delValueId);
                //urlParam    面包屑的url其实就是当前的url-选中的valueId,做个字符串处理，此处不采用字符串处理的办法，采用重写一个新的方法
                String urlParamForCrumb = getUrlParamForCrumb(pmsSearchParam, delValueId);
                pmsSearchCrumb.setUrlParam(urlParamForCrumb);
                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String valueId = pmsBaseAttrValue.getId();
                        //for (String delValueId : delValueIds) {  代码改造优化
                        if (delValueId.equals(valueId)) {
                            //面包屑的name
                            String valueName=pmsBaseAttrValue.getValueName();
                            //valueName
                            pmsSearchCrumb.setValueName(valueName);
                            //删除该属性值所在的属性列
                            iterator.remove();
                        }
                    }
                }
                //面包屑
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            modelMap.put("attrValueSelectedList", pmsSearchCrumbs);

        }
        //modelMap.put("urlParam","catalog3Id=61");
        String urlParam = getUrlParam(pmsSearchParam);
        modelMap.put("urlParam", urlParam);
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            modelMap.put("keyword", keyword);
        }

      //面包屑代码优化改造
//        if(delValueIds!=null){
//            //面包屑  valueId   valueName  urlParam
////            List<PmsSearchCrumb>pmsSearchCrumbs=new ArrayList<>();
//            for (String delValueId : delValueIds) {
//                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
//                //经过数据处理,生成面包屑的参数 valueId   valueName  urlParam
//                //valueId
//                pmsSearchCrumb.setValueId(delValueId);
//                //valueName  数据暂时没有valueName，先用valueId代替
//                pmsSearchCrumb.setValueName(delValueId);
//                //urlParam    面包屑的url其实就是当前的url-选中的valueId,做个字符串处理，此处不采用字符串处理的办法，采用重写一个新的方法
//                String urlParamForCrumb = getUrlParamForCrumb(pmsSearchParam, delValueId);
//                pmsSearchCrumb.setUrlParam(urlParamForCrumb);
//                pmsSearchCrumbs.add(pmsSearchCrumb);
//            }
//        }
        // modelMap.put("attrValueSelectedList", pmsSearchCrumbs);

        return "list";
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam) {
        String urlParams = "";
        //关键字  不确定一定有
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            urlParams = urlParams + "&keyword=" + keyword;
        }
        //三级分类id   不确定一定有  但是关键字和三级分类id一定有一个有
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        if (StringUtils.isNotBlank(catalog3Id)) {
            urlParams = urlParams + "&catalog3Id=" + catalog3Id;
        }
        //分类属性值列表
        String[] valueIds = pmsSearchParam.getValueId();
        if (valueIds != null) {
            for (String valueId : valueIds) {
                urlParams = urlParams + "&valueId=" + valueId;
            }
        }
        //去掉第一个&
        String urlParamsJ = urlParams.replaceFirst("&", "");
        return urlParamsJ;
    }

    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String delValueId) {
        String urlParams = "";
        //关键字  不确定一定有
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            urlParams = urlParams + "&keyword=" + keyword;
        }
        //三级分类id   不确定一定有  但是关键字和三级分类id一定有一个有
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        if (StringUtils.isNotBlank(catalog3Id)) {
            urlParams = urlParams + "&catalog3Id=" + catalog3Id;
        }
        //分类属性值列表
        String[] valueIds = pmsSearchParam.getValueId();
        if (valueIds != null) {
            for (String valueId : valueIds) {
                //if是当前的id，就不拼接，如果不是，就拼接
                if (!delValueId.equals(valueId)) {
                    urlParams = urlParams + "&valueId=" + valueId;
                }
            }
        }
        //去掉第一个&
        String urlParamsJ = urlParams.replaceFirst("&", "");
        return urlParamsJ;
    }
}
