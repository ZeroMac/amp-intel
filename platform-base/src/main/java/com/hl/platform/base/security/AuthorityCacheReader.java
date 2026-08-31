package com.hl.platform.base.security;

import java.util.List;

public interface AuthorityCacheReader {

    List<String> getAuthorities(String userId);
}
