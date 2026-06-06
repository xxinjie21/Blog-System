package com.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.entity.Category;

import java.util.List;

/**
 * 文章分类服务接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 获取所有分类列表
     * 
     * @return 分类列表
     */
    List<Category> listAll();
}
