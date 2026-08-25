package com.hl.platform.system.service.impl;

import com.hl.platform.system.converter.FunctionConverter;
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
    private final FunctionConverter functionConverter;

    public FunctionServiceImpl(FunctionMapper functionMapper, FunctionConverter functionConverter) {
        this.functionMapper = functionMapper;
        this.functionConverter = functionConverter;
    }

    @Override
    public List<FunctionVO> listChildrenByParentId(Long parentId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(FUNCTION.PARENTID.eq(parentId))
                .orderBy(FUNCTION.ORDERNUM.asc());

        return functionConverter.toVOList(functionMapper.selectListByQuery(queryWrapper));
    }
}
