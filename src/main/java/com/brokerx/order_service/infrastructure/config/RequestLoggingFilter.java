package com.brokerx.order_service.infrastructure.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Minimal access log filter for security auditing.
 * Masks password fields and does not log Authorization header values.
 */
@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest http) {
            String method = http.getMethod();
            String uri = http.getRequestURI();
            String userAgent = http.getHeader("User-Agent");
            String authorization = http.getHeader("Authorization") != null ? "present" : "absent";
            // Do not read body to avoid interfering with controllers.
            log.info("ACCESS method={} uri={} authHeader={} ua={}", method, uri, authorization, userAgent);
        }
        chain.doFilter(request, response);
    }
}
