package com.shopmanagement.gstservice.support;

import com.shopmanagement.gstservice.filter.RequestIdFilter;

public final class TenantIds {

    private TenantIds() {
    }

    public static long require() {
        Long tenantId = RequestIdFilter.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        return tenantId;
    }

    public static Long currentOrNull() {
        return RequestIdFilter.getCurrentTenantId();
    }
}
