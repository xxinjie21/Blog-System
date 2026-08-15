package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_permission")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 权限名称 */
    private String name;

    /** 权限编码（资源:操作，如 article:create） */
    private String code;

    /** 权限描述 */
    private String description;

    private LocalDateTime createTime;
}
