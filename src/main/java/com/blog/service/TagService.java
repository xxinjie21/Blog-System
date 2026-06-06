package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.entity.Tag;

import java.util.List;

/**
 * 文章标签服务接口
 */
public interface TagService extends IService<Tag> {

    /**
     * 获取热门标签列表
     * 
     * @param limit 数量限制
     * @return 标签列表
     */
    List<Tag> listHotTags(Integer limit);
}
