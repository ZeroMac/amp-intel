package com.hl.platform.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Date;

import java.io.Serial;


/**
 * 用户表 实体类。
 *
 * @author Mac
 * @since 2026-08-24
 */
@Table(value = "sys_user", schema = "public")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @Id
    private String userid;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 有效
     */
    private Integer enabled;

    /**
     * 删除标记
     */
    private Integer deleteflag;

    /**
     * 删除人id
     */
    private String deleterid;

    /**
     * 删除人
     */
    private String deletername;

    /**
     * 删除时间
     */
    private Date deletedate;

    /**
     * 排序号
     */
    private String ordernum;

    /**
     * 数据层级id
     */
    private String datalevelid;

    /**
     * 数据层级
     */
    private String datalevelname;

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getDeleteflag() {
        return deleteflag;
    }

    public void setDeleteflag(Integer deleteflag) {
        this.deleteflag = deleteflag;
    }

    public String getDeleterid() {
        return deleterid;
    }

    public void setDeleterid(String deleterid) {
        this.deleterid = deleterid;
    }

    public String getDeletername() {
        return deletername;
    }

    public void setDeletername(String deletername) {
        this.deletername = deletername;
    }

    public Date getDeletedate() {
        return deletedate;
    }

    public void setDeletedate(Date deletedate) {
        this.deletedate = deletedate;
    }

    public String getOrdernum() {
        return ordernum;
    }

    public void setOrdernum(String ordernum) {
        this.ordernum = ordernum;
    }

    public String getDatalevelid() {
        return datalevelid;
    }

    public void setDatalevelid(String datalevelid) {
        this.datalevelid = datalevelid;
    }

    public String getDatalevelname() {
        return datalevelname;
    }

    public void setDatalevelname(String datalevelname) {
        this.datalevelname = datalevelname;
    }

}
