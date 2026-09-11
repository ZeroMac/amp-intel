package com.hl.platform.system.service;

public interface AuthService {
    /** Delete only the current authenticated session; an absent session is already logged out. */
    void logout();
}
