package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper 接口
 *
 * 考点：MyBatis-Plus 根据用户名查询用户（登录用）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
