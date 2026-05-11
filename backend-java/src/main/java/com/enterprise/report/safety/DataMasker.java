package com.enterprise.report.safety;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Data masking service for sensitive fields.
 * Masks phone numbers, IDs, emails, bank cards, etc.
 */
@Component
public class DataMasker {

    /** Masking patterns for common sensitive data */
    private static final Map<String, MaskRule> MASK_RULES = new HashMap<>();

    static {
        // Phone: 138****1234
        MASK_RULES.put("phone", new MaskRule(
                Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})"),
                "$1****$2"
        ));
        // ID card: 110***********1234
        MASK_RULES.put("id_card", new MaskRule(
                Pattern.compile("(\\d{3})\\d{11}(\\d{4}[0-9Xx]?)"),
                "$1***********$2"
        ));
        // Email: t***@example.com
        MASK_RULES.put("email", new MaskRule(
                Pattern.compile("(.{1,2})[^@]*(@.+)"),
                "$1***$2"
        ));
        // Bank card: 6222 **** **** 1234
        MASK_RULES.put("bank_card", new MaskRule(
                Pattern.compile("(\\d{4})\\d{8,12}(\\d{4})"),
                "$1 **** **** $2"
        ));
        // Name: 张*三 or 张*
        MASK_RULES.put("name", new MaskRule(null, null) {
            @Override
            public String apply(String value) {
                if (value == null || value.length() < 2) return "*";
                if (value.length() == 2) return value.charAt(0) + "*";
                return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
            }
        });
        // Address: 北京市****
        MASK_RULES.put("address", new MaskRule(
                Pattern.compile("(.{2,6}).*"),
                "$1****"
        ));
    }

    /**
     * Mask a value based on its field type.
     */
    public String mask(String value, String fieldType) {
        if (value == null || value.isEmpty()) return value;
        
        MaskRule rule = MASK_RULES.get(fieldType.toLowerCase());
        if (rule == null) {
            // Default: mask middle portion
            return defaultMask(value);
        }
        return rule.apply(value);
    }

    /**
     * Auto-detect and mask sensitive data in a value.
     */
    public String autoMask(String value) {
        if (value == null || value.isEmpty()) return value;

        // Try to detect type
        if (value.matches("1[3-9]\\d{9}")) return mask(value, "phone");
        if (value.matches("\\d{15}(\\d{3}[0-9Xx]?)?")) return mask(value, "id_card");
        if (value.matches("[\\w.]+@[\\w.]+")) return mask(value, "email");
        if (value.matches("\\d{16,19}")) return mask(value, "bank_card");
        
        return value;
    }

    /**
     * Mask all values in a row based on column metadata.
     */
    public Map<String, Object> maskRow(Map<String, Object> row, Map<String, String> columnTypes) {
        Map<String, Object> masked = new HashMap<>(row);
        for (Map.Entry<String, String> entry : columnTypes.entrySet()) {
            String col = entry.getKey();
            String type = entry.getValue();
            if (masked.containsKey(col) && masked.get(col) instanceof String) {
                masked.put(col, mask((String) masked.get(col), type));
            }
        }
        return masked;
    }

    private String defaultMask(String value) {
        if (value.length() <= 4) return "****";
        int showLen = Math.max(2, value.length() / 4);
        return value.substring(0, showLen) + "*".repeat(value.length() - showLen * 2) + value.substring(value.length() - showLen);
    }

    static class MaskRule {
        final Pattern pattern;
        final String replacement;

        MaskRule(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        String apply(String value) {
            if (pattern == null) return value;
            return pattern.matcher(value).replaceAll(replacement);
        }
    }
}
