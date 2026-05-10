package com.enterprise.report.util;

import java.util.regex.Pattern;

/**
 * Utility for masking sensitive data in API responses.
 * Supports masking of common sensitive fields like phone, email, ID card, etc.
 */
public class DataMaskingUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(.{1,3})[^@]*(@.*)");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}(\\d{4})");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})\\d+(\\d{4})");

    /**
     * Mask phone number: 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) return phone;
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    /**
     * Mask email: j***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) return email;
        return EMAIL_PATTERN.matcher(email).replaceAll("$1***$2");
    }

    /**
     * Mask ID card: 110101********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) return idCard;
        return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1********$2");
    }

    /**
     * Mask bank card: 6222****1234
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.isEmpty()) return bankCard;
        return BANK_CARD_PATTERN.matcher(bankCard).replaceAll("$1****$2");
    }

    /**
     * Generic string masking: show first and last character, mask the rest
     */
    public static String maskGeneric(String value) {
        if (value == null || value.isEmpty()) return value;
        if (value.length() <= 2) return "*".repeat(value.length());
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }

    /**
     * Mask password: always returns ********
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) return password;
        return "********";
    }
}
