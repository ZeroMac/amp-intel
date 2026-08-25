package com.hl.platform.gateway.cache.model;

/**
 * Cached function metadata aligned with system-service's FunctionVO.
 */
public record FunctionCacheItem(
        Long funcId,
        Long parentId,
        String funcName,
        String funcTitle,
        String funcUrl,
        Integer funcType,
        String orderNum) {
}
