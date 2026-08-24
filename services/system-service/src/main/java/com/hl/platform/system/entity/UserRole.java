package com.hl.platform.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;

import java.io.Serial;


/**
 * 角色人员关系表 实体类。
 *
 * @author Mac
 * @since 2026-08-24
 */
@Table(value = "sys_user_role", schema = "public")
public class UserRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String userid;

    @Id
    private Long roleid;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public Long getRoleid() {
        return roleid;
    }

    public void setRoleid(Long roleid) {
        this.roleid = roleid;
    }

}
