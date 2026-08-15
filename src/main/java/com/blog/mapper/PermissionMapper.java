package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限 Mapper
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户 ID 查询该用户拥有的全部权限编码
     *
     * SQL 链路：user → user_role → role_permission → permission
     */
    @Select("""
        SELECT DISTINCT p.code
        FROM blog_user_role ur
        JOIN blog_role_permission rp ON ur.role_id = rp.role_id
        JOIN blog_permission p ON rp.permission_id = p.id
        WHERE ur.user_id = #{userId}
    """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
