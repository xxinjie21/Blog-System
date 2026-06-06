package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章分类 Mapper 接口
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
