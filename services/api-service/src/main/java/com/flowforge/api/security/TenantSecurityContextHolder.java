package com.flowforge.api.security;

public class TenantSecurityContextHolder {

    private static final ThreadLocal<TenantSecurityContext> CONTEXT = new ThreadLocal<>();

    private TenantSecurityContextHolder() {}

    public static void setContext(TenantSecurityContext context) {
        CONTEXT.set(context);
    }

    public static TenantSecurityContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
