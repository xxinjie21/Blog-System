package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.service.TagService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文章标签服务实现类
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public List<Tag> listHotTags(Integer limit) {
        // 按文章数量降序排列，获取热门标签
        return list(new QueryWrapper<Tag>()
                .orderByDesc("article_count")
                .last("LIMIT " + (limit != null ? limit : 10)));
    }
}
