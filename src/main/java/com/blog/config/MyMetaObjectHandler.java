package com.blog.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * 
 * 【面试考点】
 * 1. 自动填充原理
 * 2. MetaObjectHandler 接口
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入自动填充...");
        
        // 自动填充创建时间
        if (getFieldValByName("createTime", metaObject) == null) {
            setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        }
        
        // 自动填充更新时间
        if (getFieldValByName("updateTime", metaObject) == null) {
            setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新自动填充...");
        
        // 自动填充更新时间
        setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}
