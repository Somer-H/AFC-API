package com.afcgym.platform.utils;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class SecureTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecureTokenGenerator() {
    }

    public static String generate(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }
}
