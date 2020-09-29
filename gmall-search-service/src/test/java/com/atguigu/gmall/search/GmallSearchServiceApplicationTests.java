package com.atguigu.gmall.search;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sound.midi.Soundbank;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

    @Reference
    SkuService skuService; //查询sql

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() throws IOException {
        //jest的del的工具
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();

        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //term
        TermQueryBuilder termQueryBuilder1=new TermQueryBuilder("skuAttrValueList.valueId","84");
        //term
        TermQueryBuilder termQueryBuilder2=new TermQueryBuilder("skuAttrValueList.valueId","81");
        //term
        TermQueryBuilder termQueryBuilder3=new TermQueryBuilder("skuAttrValueList.valueId","43");
        //fifter
        boolQueryBuilder.filter(termQueryBuilder1);
        boolQueryBuilder.filter(termQueryBuilder2);
        boolQueryBuilder.filter(termQueryBuilder3);
        //terms
        TermsQueryBuilder termsQueryBuilder=new TermsQueryBuilder("skuAttrValueList.valueId","48","51","39");
        boolQueryBuilder.filter(termsQueryBuilder);

        //match
        MatchQueryBuilder matchQueryBuilder=new MatchQueryBuilder("skuName","诺基亚 NOKIA C3");
        //must
        boolQueryBuilder.must(matchQueryBuilder);
        //query
        searchSourceBuilder.query(boolQueryBuilder);
        //from
        searchSourceBuilder.from(0);
        //size
        searchSourceBuilder.size(20);
        //highlight
        searchSourceBuilder.highlight(null);
        String delStr=searchSourceBuilder.toString();
        System.out.println(delStr);
        //用API执行复杂查询
        //new Search.Builder(null).addIndex(null).addType(null).build();
        Search search = new Search.Builder(delStr).addIndex("gmall0105").addType("PmsSkuInfo").build();
        SearchResult execute = jestClient.execute(search);
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);
        List<PmsSearchSkuInfo>pmsSearchSkuInfos=new ArrayList<>();
        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
           PmsSearchSkuInfo pmsSearchSkuInfo=hit.source;
           pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }
        System.out.println(pmsSearchSkuInfos.size());

    }

    public void put() throws IOException {
        //查询mysql数据
        List<PmsSkuInfo> pmsSkuInfoList=new ArrayList<>();
        pmsSkuInfoList=skuService.getAllSku("61");
        //转化为es的数据结构
        List<PmsSearchSkuInfo>pmsSearchSkuInfos=new ArrayList<>();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
            PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
            BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);
            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }
        //导入es
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall0105").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId()).build();
            jestClient.execute(put);
        }


    }

    /**
     *   //用API执行复杂查询
     *         //new Search.Builder(null).addIndex(null).addType(null).build();
     *         Search search = new Search.Builder("{\n" +
     *                 "  \"query\": {\n" +
     *                 "    \"bool\": {\n" +
     *                 "      \"filter\": [\n" +
     *                 "       {\n" +
     *                 "       \"terms\":{\"skuAttrValueList.valueId\":[\"48,51\",\"39\"]}\n" +
     *                 "       },\n" +
     *                 "       {\n" +
     *                 "         \"term\":{\"skuAttrValueList.valueId\":\"84\"}\n" +
     *                 "       },\n" +
     *                 "       {\n" +
     *                 "         \"term\":{\"skuAttrValueList.valueId\":\"81\"}\n" +
     *                 "       },\n" +
     *                 "       {\n" +
     *                 "         \"term\":{\"skuAttrValueList.valueId\":\"43\"}\n" +
     *                 "       }\n" +
     *                 "       ] , \n" +
     *                 "     \"must\": [\n" +
     *                 "       {\n" +
     *                 "         \"match\": {\n" +
     *                 "           \"skuName\": \"诺基亚 NOKIA C3\"\n" +
     *                 "         }\n" +
     *                 "       }\n" +
     *                 "     ]\n" +
     *                 "    }\n" +
     *                 "  }\n" +
     *                 "}").addIndex("gmall0105").addType("PmsSkuInfo").build();
     *         SearchResult execute = jestClient.execute(search);
     *         List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = execute.getHits(PmsSearchSkuInfo.class);
     *         List<PmsSearchSkuInfo>pmsSearchSkuInfos=new ArrayList<>();
     *         for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
     *            PmsSearchSkuInfo pmsSearchSkuInfo=hit.source;
     *            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
     *         }
     *         System.out.println(pmsSearchSkuInfos.size());
     */

    /**
     *  //jest的del的工具//查询del的封装工具类
     *         SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();
     *
     *         //bool
     *         BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
     *         //fifter
     *         boolQueryBuilder.filter(null);
     *         //must
     *         boolQueryBuilder.must(null);
     *         //query
     *         searchSourceBuilder.query(boolQueryBuilder);
     *         //from
     *         searchSourceBuilder.from(0);
     *         //size
     *         searchSourceBuilder.size(20);
     *         //highlight
     *         searchSourceBuilder.highlight(null);
     */

}
