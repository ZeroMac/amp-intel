package com.hl.platform.system.converter;

import com.hl.platform.system.entity.Function;
import com.hl.platform.system.vo.FunctionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 功能项对象转换器。
 */
@Mapper(componentModel = "spring")
public interface FunctionConverter {

    @Mapping(source = "funcid", target = "funcId")
    @Mapping(source = "parentid", target = "parentId")
    @Mapping(source = "funcname", target = "funcName")
    @Mapping(source = "functitle", target = "funcTitle")
    @Mapping(source = "funcurl", target = "funcUrl")
    @Mapping(source = "functype", target = "funcType")
    @Mapping(source = "ordernum", target = "orderNum")
    FunctionVO toVO(Function entity);

    List<FunctionVO> toVOList(List<Function> entities);
}
