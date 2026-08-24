package com.hl.platform.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;

import java.io.Serial;


/**
 * 角色表 实体类。
 *
 * @author Mac
 * @since 2026-08-24
 */
@Table(value = "sys_role", schema = "public")
public class Role implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色id
     */
    @Id
    private Long roleid;

    /**
     * 父项id
     */
    private Long groupid;

    /**
     * 是否角色组
     */
    private Integer isgroup;

    /**
     * 角色编号
     */
    private String roleno;

    private String classid;

    /**
     * 角色名
     */
    private String rolename;

    /**
     * 备注
     */
    private String remark;

    public Long getRoleid() {
        return roleid;
    }

    public void setRoleid(Long roleid) {
        this.roleid = roleid;
    }

    public Long getGroupid() {
        return groupid;
    }

    public void setGroupid(Long groupid) {
        this.groupid = groupid;
    }

    public Integer getIsgroup() {
        return isgroup;
    }

    public void setIsgroup(Integer isgroup) {
        this.isgroup = isgroup;
    }

    public String getRoleno() {
        return roleno;
    }

    public void setRoleno(String roleno) {
        this.roleno = roleno;
    }

    public String getClassid() {
        return classid;
    }

    public void setClassid(String classid) {
        this.classid = classid;
    }

    public String getRolename() {
        return rolename;
    }

    public void setRolename(String rolename) {
        this.rolename = rolename;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

}
