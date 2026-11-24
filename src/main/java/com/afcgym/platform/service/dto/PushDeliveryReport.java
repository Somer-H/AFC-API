package com.afcgym.platform.service.dto;

import java.util.Set;

public record PushDeliveryReport(Set<String> acceptedTokens, Set<String> unregisteredTokens) {}
