package com.afcgym.platform.web.dto.response;

import java.time.Instant;

public record PushTokenResponse(Long id, String token, Instant registeredAt) {}
