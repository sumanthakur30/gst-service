package com.shopmanagement.gstservice.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            if (!"OPTIONS".equalsIgnoreCase(request.getMethod()) && !skipsTenantContext(request.getRequestURI())) {
                String tenantId = request.getHeader(TENANT_ID_HEADER);
                if (tenantId == null || tenantId.isBlank()) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Missing tenant context header: X-Tenant-Id\"}");
                    return;
                }
                try {
                    currentTenantId.set(Long.valueOf(tenantId.trim()));
                } catch (NumberFormatException ex) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Invalid X-Tenant-Id header\"}");
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY);
            currentTenantId.remove();
        }
    }

    public static Long getCurrentTenantId() {
        return currentTenantId.get();
    }

    public static String getCurrentRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }

    private static boolean skipsTenantContext(String uri) {
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/webjars");
    }
}
