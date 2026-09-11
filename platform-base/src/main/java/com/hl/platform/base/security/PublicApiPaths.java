package com.hl.platform.base.security;

/** Public endpoints have exactly one service segment and one endpoint segment. */
public final class PublicApiPaths {
    public static final String GATEWAY = "/api/*/public/*";
    public static final String SERVICE = "/*/public/*";

    private PublicApiPaths() { }
}
