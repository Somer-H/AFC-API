package com.afcgym.platform.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacSha256 {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSha256() {
    }

    public static String hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is not available", exception);
        }
    }

    public static boolean matches(String secret, String message, String expectedHex) {
        byte[] computed = hex(secret, message).getBytes(StandardCharsets.UTF_8);
        byte[] expected = expectedHex.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(computed, expected);
    }
}
