package com.afcgym.platform.service.dto;

import org.springframework.core.io.Resource;

public record StoredVideo(Resource resource, String contentType) {}
