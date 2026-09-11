package com.hl.platform.base.security;

/** Session metadata attached to the restored Authentication. */
public record SessionIdentity(String sid, long tokenVersion) { }
