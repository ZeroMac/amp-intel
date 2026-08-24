package com.hl.platform.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;

import java.io.Serial;


/**
 * 功能项定义 实体类。
 *
 * @author Mac
 * @since 2026-08-24
 */
@Table(value = "sys_function", schema = "public")
public class Function implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 功能项id
     */
    @Id
    private Long funcid;

    /**
     * 父项id
     */
    private Long parentid;

    /**
     * 功能项名
     */
    private String funcname;

    /**
     * 功能项标题
     */
    private String functitle;

    /**
     * 地址
     */
    private String funcurl;

    /**
     * 类型
     */
    private Integer functype;

    /**
     * 排序号
     */
    private String ordernum;

    /**
     * 模块id
     */
    private String funcmoduleid;

    /**
     * 备注
     */
    private String remark;

    /**
     * 模块名
     */
    private String funcmodulename;

    public Long getFuncid() {
        return funcid;
    }

    public void setFuncid(Long funcid) {
        this.funcid = funcid;
    }

    public Long getParentid() {
        return parentid;
    }

    public void setParentid(Long parentid) {
        this.parentid = parentid;
    }

    public String getFuncname() {
        return funcname;
    }

    public void setFuncname(String funcname) {
        this.funcname = funcname;
    }

    public String getFunctitle() {
        return functitle;
    }

    public void setFunctitle(String functitle) {
        this.functitle = functitle;
    }

    public String getFuncurl() {
        return funcurl;
    }

    public void setFuncurl(String funcurl) {
        this.funcurl = funcurl;
    }

    public Integer getFunctype() {
        return functype;
    }

    public void setFunctype(Integer functype) {
        this.functype = functype;
    }

    public String getOrdernum() {
        return ordernum;
    }

    public void setOrdernum(String ordernum) {
        this.ordernum = ordernum;
    }

    public String getFuncmoduleid() {
        return funcmoduleid;
    }

    public void setFuncmoduleid(String funcmoduleid) {
        this.funcmoduleid = funcmoduleid;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getFuncmodulename() {
        return funcmodulename;
    }

    public void setFuncmodulename(String funcmodulename) {
        this.funcmodulename = funcmodulename;
    }

}
