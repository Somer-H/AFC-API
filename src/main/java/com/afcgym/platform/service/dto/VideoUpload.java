package com.afcgym.platform.service.dto;

import java.io.InputStream;

public record VideoUpload(InputStream content, String contentType, long size) {}
