package com.aigui.gmall.user.mapper;

import com.aigui.gmall.user.bean.UmsMember;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface UserMapper extends Mapper<UmsMember> {

    List<UmsMember> selectAllUser();
}
