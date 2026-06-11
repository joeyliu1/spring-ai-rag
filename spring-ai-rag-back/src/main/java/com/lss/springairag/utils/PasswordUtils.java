package com.lss.springairag.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

public final class PasswordUtils {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private PasswordUtils() {
    }

    public static String encode(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encodedPassword)) {
            return false;
        }
        if (isBcrypt(encodedPassword)) {
            return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
        }
        return legacyMd5(rawPassword).equals(encodedPassword);
    }

    public static boolean needsUpgrade(String encodedPassword) {
        return StringUtils.hasText(encodedPassword) && !isBcrypt(encodedPassword);
    }

    private static boolean isBcrypt(String encodedPassword) {
        return encodedPassword.startsWith("$2a$")
                || encodedPassword.startsWith("$2b$")
                || encodedPassword.startsWith("$2y$");
    }

    private static String legacyMd5(String rawPassword) {
        return DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
    }
}
