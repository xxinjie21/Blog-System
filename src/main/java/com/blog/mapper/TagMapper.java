package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章标签 Mapper 接口
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
