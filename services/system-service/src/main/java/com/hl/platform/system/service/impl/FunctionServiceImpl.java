package com.hl.platform.system.service.impl;

import com.hl.platform.system.entity.Function;
import com.hl.platform.system.mapper.FunctionMapper;
import com.hl.platform.system.service.FunctionService;
import com.hl.platform.system.vo.FunctionVO;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hl.platform.system.entity.table.FunctionTableDef.FUNCTION;

/**
 * 功能项服务实现。
 */
@Service
public class FunctionServiceImpl implements FunctionService {

    private final FunctionMapper functionMapper;

    public FunctionServiceImpl(FunctionMapper functionMapper) {
        this.functionMapper = functionMapper;
    }

    @Override
    public List<FunctionVO> listChildrenByParentId(Long parentId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(FUNCTION.PARENTID.eq(parentId))
                .orderBy(FUNCTION.ORDERNUM.asc());

        return functionMapper.selectListByQuery(queryWrapper).stream()
                .map(this::toVO)
                .toList();
    }

    private FunctionVO toVO(Function function) {
        FunctionVO functionVO = new FunctionVO();
        functionVO.setFuncId(function.getFuncid());
        functionVO.setParentId(function.getParentid());
        functionVO.setFuncName(function.getFuncname());
        functionVO.setFuncTitle(function.getFunctitle());
        functionVO.setFuncUrl(function.getFuncurl());
        functionVO.setFuncType(function.getFunctype());
        functionVO.setOrderNum(function.getOrdernum());
        return functionVO;
    }
}
