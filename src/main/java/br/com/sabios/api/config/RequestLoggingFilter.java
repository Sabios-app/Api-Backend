package br.com.sabios.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final int MAX_PAYLOAD_LENGTH = 1000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestResponse(wrappedRequest, wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequestResponse(ContentCachingRequestWrapper request,
                                    ContentCachingResponseWrapper response,
                                    long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";
        String fullPath = uri + queryString;
        int status = response.getStatus();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName()
                : "anonymous";

        String clientIp = getClientIp(request);
        String responseBody = extractBody(response.getContentAsByteArray(), response.getContentType());

        // Sanitiza respostas com tokens ou senhas se houver
        if (uri.contains("/auth/login") || uri.contains("/auth/register") || uri.contains("/auth/reset-password")) {
            responseBody = "[REDACTED - AUTH DATA]";
        }

        log.info("[HTTP] {} {} | Status: {} | Tempo: {}ms | IP: {} | User: {} | Resposta: {}",
                method, fullPath, status, duration, clientIp, user, responseBody);
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractBody(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            return "-";
        }
        if (contentType != null && !contentType.contains("application/json") && !contentType.contains("text/")) {
            return "[binary/non-text: " + content.length + " bytes]";
        }
        String body = new String(content, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        if (body.length() > MAX_PAYLOAD_LENGTH) {
            return body.substring(0, MAX_PAYLOAD_LENGTH) + "... [truncated]";
        }
        return body;
    }
}
