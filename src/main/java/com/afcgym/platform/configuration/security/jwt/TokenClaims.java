package com.afcgym.platform.configuration.security.jwt;

import org.springframework.security.oauth2.jwt.Jwt;

public final class TokenClaims {

    public static final String USER_ID = "userId";
    public static final String ROLE = "role";
    public static final String SESSION_ID = "sid";
    public static final String SESSION_STARTED_AT = "auth_time";

    private TokenClaims() {
    }

    public static String sessionId(Jwt jwt) {
        String sessionId = jwt.getClaimAsString(SESSION_ID);
        return sessionId != null ? sessionId : jwt.getId();
    }
}
