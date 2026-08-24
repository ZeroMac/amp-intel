package com.hl.platform.system.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;

import java.io.Serial;


/**
 * 权限分配表 实体类。
 *
 * @author Mac
 * @since 2026-08-24
 */
@Table(value = "sys_right", schema = "public")
public class Right implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long roleid;

    @Id
    private Long funcid;

    public Long getRoleid() {
        return roleid;
    }

    public void setRoleid(Long roleid) {
        this.roleid = roleid;
    }

    public Long getFuncid() {
        return funcid;
    }

    public void setFuncid(Long funcid) {
        this.funcid = funcid;
    }

}
