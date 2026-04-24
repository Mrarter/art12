package com.shiyiju.admin.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

/**
 * 角色菜单关联Mapper
 */
@Mapper
public interface AdminRoleMenuMapper {

    @Insert("<script>" +
            "INSERT INTO admin_role_menu (role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>" +
            "(#{roleId}, #{menuId})" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    @Delete("DELETE FROM admin_role_menu WHERE role_id = #{roleId}")
    void deleteByRoleId(Long roleId);

    @Select("SELECT menu_id FROM admin_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(Long roleId);

    @Select("SELECT COUNT(1) FROM admin_role_menu WHERE role_id = #{roleId}")
    int countByRoleId(Long roleId);
}
