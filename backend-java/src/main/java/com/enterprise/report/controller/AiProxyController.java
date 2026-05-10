package com.enterprise.report.controller;

import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;

/**
 * Proxy controller that forwards AI-related requests to the Python AI service.
 * This allows the frontend to call /api/ai/* endpoints through the Java backend.
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiProxyController {

    @Value("${ai.service.url:http://localhost:8081}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;

    @PostMapping("/nl2sql")
    public ResponseEntity<String> nl2sql(@RequestBody String body, HttpServletRequest request) {
        return proxy("/api/ai/nl2sql", body, request);
    }

    @PostMapping("/nl2sql/validate")
    public ResponseEntity<String> nl2sqlValidate(@RequestBody String body, HttpServletRequest request) {
        return proxy("/api/ai/nl2sql/validate", body, request);
    }

    @PostMapping("/nl2sql/execute")
    public ResponseEntity<String> nl2sqlExecute(@RequestBody String body, HttpServletRequest request) {
        return proxy("/api/ai/nl2sql/execute", body, request);
    }

    @PostMapping("/analysis")
    public ResponseEntity<String> analysis(@RequestBody String body, HttpServletRequest request) {
        return proxy("/api/ai/analysis", body, request);
    }

    @PostMapping("/suggest-columns")
    public ResponseEntity<String> suggestColumns(@RequestBody String body, HttpServletRequest request) {
        return proxy("/api/ai/suggest-columns", body, request);
    }

    @PostMapping("/suggest-expression")
    public ResponseEntity<String> suggestExpression(@RequestBody String body, HttpServletRequest request) {
        return proxy("/api/ai/suggest-expression", body, request);
    }

    @PostMapping("/optimize-prompt")
    public ResponseEntity<String> optimizePrompt(@RequestBody String body, HttpServletRequest request) {
        return proxy("/api/ai/optimize-prompt", body, request);
    }

    @PostMapping("/generate-report-template")
    public ResponseEntity<String> generateReportTemplate(@RequestBody String body, HttpServletRequest request) {
        return proxy("/api/ai/generate-report-template", body, request);
    }

    private ResponseEntity<String> proxy(String path, String body, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Forward relevant headers
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                if (name.equalsIgnoreCase("authorization") || name.equalsIgnoreCase("x-tenant-id")) {
                    headers.set(name, request.getHeader(name));
                }
            }

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            String url = aiServiceUrl + path;

            log.debug("Proxying AI request to: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody());
        } catch (Exception e) {
            log.error("AI proxy request failed: {}", e.getMessage());
            throw new BusinessException(502, "AI service unavailable: " + e.getMessage());
        }
    }
}
