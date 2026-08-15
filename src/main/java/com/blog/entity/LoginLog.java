package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录审计日志实体
 *
 * 【企业级考点】
 * 1. 审计日志：登录成功/失败/登出/改密/刷新全部留痕，安全追溯的依据
 * 2. create_time 由数据库 DEFAULT CURRENT_TIMESTAMP 填充，无需代码干预
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_login_log")
public class LoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志 ID - 主键自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 ID（登录失败时可能为空）
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 操作类型：REGISTER/LOGIN_SUCCESS/LOGIN_FAIL/LOGOUT/REFRESH/CHANGE_PASSWORD
     */
    private String operation;

    /**
     * 客户端 IP
     */
    private String ipAddress;

    /**
     * 详情（如失败原因）
     */
    private String detail;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
