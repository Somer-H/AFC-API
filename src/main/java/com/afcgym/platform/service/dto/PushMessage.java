package com.afcgym.platform.service.dto;

import java.util.Map;

public record PushMessage(String to, String title, String body, String sound, Map<String, String> data) {}
