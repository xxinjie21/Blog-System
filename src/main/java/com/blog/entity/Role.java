package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_role")
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 角色名称 */
    private String name;

    /** 角色编码（admin/editor/user） */
    private String code;

    /** 角色描述 */
    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
