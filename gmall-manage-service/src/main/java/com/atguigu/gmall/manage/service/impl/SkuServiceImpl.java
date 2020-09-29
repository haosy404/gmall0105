package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        //插入skuInfo
        int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();
        //插入平台属性关联
        List<PmsSkuAttrValue> pmsSkuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : pmsSkuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }
        //插入销售属性关联
        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : pmsSkuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }
        //插入图片信息
        List<PmsSkuImage> pmsSkuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : pmsSkuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
    }
    //缓存
    public PmsSkuInfo getSkuByIdFromDb(String skuId) {

        //sku商品对象
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        //sku的图片集合做的有点问题，暂时注释，估计是数据库数据有问题
        //sku的图片集合
//        PmsSkuImage pmsSkuImage = new PmsSkuImage();
//        pmsSkuImage.setSkuId(skuId);
//        pmsSkuImage.setSku_id(skuId);
//        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
//        pmsSkuInfo1.setSkuImageList(pmsSkuImages);

        return pmsSkuInfo1;
    }
    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        /**
         * 缓存常见问题：
         * 缓存击穿
         * 缓存雪崩
         * 缓存穿透
         */
        //sku商品对象
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        //链接缓存
        Jedis jedis = redisUtil.getJedis();
        //查询缓存
        String skuKey="sku:"+skuId+":info";
        String skuJson = jedis.get(skuKey);
        if(StringUtils.isNotBlank(skuJson)){
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else {
            //如果缓存中没有，查询mysql
            //设置分布式锁
            String token = UUID.randomUUID().toString();
            String OK = jedis.set("sku:" + skuId + ":lock", token, "nx", "px", 10000);
            if(StringUtils.isNotBlank(OK)&&OK.equals("OK")){
                //先查缓存再查数据库
                pmsSkuInfo=getSkuByIdFromDb(skuId);
                if(pmsSkuInfo!=null){
                    jedis.set("sku:"+skuId+":info",JSON.toJSONString(pmsSkuInfo));
                }else{
                    //解决缓存穿透的问题
                    jedis.setex("sku:"+skuId+":info",60*3,JSON.toJSONString(""));
                }
                //在访问mysql之后，删除锁
                String lockToken = jedis.get("sku:" + skuId + ":lock");
                //是自己的锁再删除，不要删除别人的锁
                //例如:几个人拿到的锁的名字一样，这样为了解决这个冲突，可以把k，v中的v用一个随机的数字token来标识。
                if(StringUtils.isNotBlank(lockToken)&&lockToken.equals(token)) {
                    jedis.del("sku:" + skuId + ":lock");//用token确定是自己的sku的锁。
                }
            }else{
                //设置失败，自旋
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //注意这里这个return超级重要，如果没有这个return 就相当于是开通了2个线程，如果有这个return就相当于是一个线程。
                return getSkuById(skuId);
            }

        }

        //关闭redis
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos=pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku(String catalog3Id) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();
            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> select = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(select);
        }
        return pmsSkuInfos;
    }
}
