package com.blog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户-角色关联 Mapper
 */
@Mapper
public interface UserRoleMapper {

    /**
     * 给用户分配默认角色（注册时调用）
     */
    @Insert("INSERT IGNORE INTO blog_user_role (user_id, role_id) " +
            "SELECT #{userId}, id FROM blog_role WHERE code = #{roleCode}")
    void assignRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
