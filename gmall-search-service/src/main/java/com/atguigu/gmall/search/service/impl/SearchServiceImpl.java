package com.atguigu.gmall.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    JestClient jestClient;

    @Override
    public List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam) {
        String delStr=getSearchDsl(pmsSearchParam);
        System.out.println(delStr);
        Search search = new Search.Builder(delStr).addIndex("gmall0105").addType("PmsSkuInfo").build();
        SearchResult execute = null;
        try {
            execute = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);
        List<PmsSearchSkuInfo>pmsSearchSkuInfos=new ArrayList<>();
        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo pmsSearchSkuInfo=hit.source;
            Map<String,List<String>>highlight=hit.highlight;
            //必须判断是否为空，因为高亮显示是在搜索框搜索才有的，如果没有搜索，只是点击下面的面包屑的话，会报空指针异常。
            if(highlight!=null) {
                String skuName = highlight.get("skuName").get(0);
                pmsSearchSkuInfo.setSkuName(skuName);
            }
            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }
        System.out.println(pmsSearchSkuInfos.size());
        return pmsSearchSkuInfos;
    }

    private String getSearchDsl(PmsSearchParam pmsSearchParam) {
        String[] skuAttrValueList = pmsSearchParam.getValueId();
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        //jest的del的工具
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();

        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        if(StringUtils.isNotBlank(catalog3Id)) {
            //term
            TermQueryBuilder termQueryBuilder= new TermQueryBuilder("catalog3Id", catalog3Id);
            //fifter
            boolQueryBuilder.filter(termQueryBuilder);
        }
        if(skuAttrValueList!=null) {
            for (String pmsSkuAttrValue : skuAttrValueList) {
                //term
                TermQueryBuilder termQueryBuilder= new TermQueryBuilder("skuAttrValueList.valueId", pmsSkuAttrValue);
                //fifter
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //must
        if(StringUtils.isNotBlank(keyword)) {
            //match
            MatchQueryBuilder matchQueryBuilder=new MatchQueryBuilder("skuName",keyword);
            //must
            boolQueryBuilder.must(matchQueryBuilder);
        }
        //query
        searchSourceBuilder.query(boolQueryBuilder);
        //高亮
        //highlight
        HighlightBuilder highlightBuilder=new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);
        //from
        searchSourceBuilder.from(0);
        //size
        searchSourceBuilder.size(20);
        //sort  排序
        searchSourceBuilder.sort("id",SortOrder.DESC);
        //聚合 aggs
        TermsBuilder group_attr= AggregationBuilders.terms("group_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(group_attr);
        String delStr=searchSourceBuilder.toString();
        return delStr;
    }
}
