package com.hl.platform.system.service;

import com.hl.platform.system.vo.FunctionVO;

import java.util.List;

/**
 * 功能项服务。
 */
public interface FunctionService {

    /**
     * 查询指定功能项的直接子节点。
     *
     * @param parentId 父功能项 ID
     * @return 按排序号排列的直接子节点
     */
    List<FunctionVO> listChildrenByParentId(Long parentId);
}
