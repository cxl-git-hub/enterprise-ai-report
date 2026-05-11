package com.enterprise.report.controller;

import com.enterprise.report.dto.ApiResponse;
import com.enterprise.report.exception.BusinessException;
import com.enterprise.report.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/share-links")
@RequiredArgsConstructor
public class ShareLinkController {

    // In production, this would use a ShareLinkService with database persistence
    // For now, generate shareable links with metadata

    /**
     * Create a share link for a report.
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createShareLink(@RequestBody Map<String, Object> request) {
        Long tenantId = TenantContext.getTenantId();
        String refType = (String) request.getOrDefault("refType", "report_output");
        String refId = (String) request.get("refId");
        String expiry = (String) request.getOrDefault("expiry", "7d");
        Boolean includeDisclaimer = (Boolean) request.getOrDefault("includeDisclaimer", true);

        if (refId == null) {
            throw new BusinessException(400, "refId is required");
        }

        // Generate a unique token
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String shareUrl = "/shared/" + refType + "/" + refId + "?token=" + token;

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("url", shareUrl);
        result.put("refType", refType);
        result.put("refId", refId);
        result.put("expiry", expiry);
        result.put("includeDisclaimer", includeDisclaimer);
        result.put("createdAt", java.time.LocalDateTime.now().toString());

        // In production: persist to share_link table
        return ApiResponse.success(result);
    }

    /**
     * Get shared report by token (public endpoint).
     */
    @GetMapping("/{refType}/{refId}")
    public ApiResponse<Map<String, Object>> getShared(
            @PathVariable String refType,
            @PathVariable String refId,
            @RequestParam String token) {
        // In production: validate token against share_link table
        Map<String, Object> result = new HashMap<>();
        result.put("refType", refType);
        result.put("refId", refId);
        result.put("token", token);
        result.put("valid", true);
        return ApiResponse.success(result);
    }
}
