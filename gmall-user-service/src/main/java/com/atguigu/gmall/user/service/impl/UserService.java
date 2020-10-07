package com.atguigu.gmall.user.service.impl;





import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.IUserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserService implements IUserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMembers= userMapper.selectAll();//userMapper.selectAllUser();
        return umsMembers;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress=new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List< UmsMemberReceiveAddress>UmsMemberReceiveAddres  = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return UmsMemberReceiveAddres;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        //先读取缓存，再读取DB
        Jedis jedis=null;
        try{
            jedis=redisUtil.getJedis();
            if(jedis!=null){
                //缓存中有读取缓存
                String umsMemberStr = jedis.get("user:" + umsMember.getPassword() + ":info");
                if(StringUtils.isNotBlank(umsMemberStr)) {
                    UmsMember umsMemberFromCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                }
            }
            //缓存中没有，读取数据库
            UmsMember umsMemberFormDb=loginFromDb(umsMember);
            if(umsMemberFormDb!=null){
                //存入缓存
                jedis.setex("user:"+umsMember.getPassword()+":info",60*60*24, JSON.toJSONString(umsMemberFormDb));
            }
            return umsMemberFormDb;

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            jedis.close();
        }
        return null;

    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis=redisUtil.getJedis();
        jedis.setex("user:"+memberId+":token",60*60*2,token);
        jedis.close();

    }

    private UmsMember loginFromDb(UmsMember umsMember) {
        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if(umsMembers!=null){
            return umsMembers.get(0);
        }
        return null;
    }

}
