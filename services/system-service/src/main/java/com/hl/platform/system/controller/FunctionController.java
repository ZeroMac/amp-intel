package com.hl.platform.system.controller;

import com.hl.platform.system.service.FunctionService;
import com.hl.platform.system.vo.FunctionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 功能项接口。
 */
@RestController
@RequestMapping("/sys/functions")
public class FunctionController {

    private final FunctionService functionService;

    public FunctionController(FunctionService functionService) {
        this.functionService = functionService;
    }

    @GetMapping("/{parentId}/children")
    public List<FunctionVO> listChildren(@PathVariable Long parentId) {
        return functionService.listChildrenByParentId(parentId);
    }
}
