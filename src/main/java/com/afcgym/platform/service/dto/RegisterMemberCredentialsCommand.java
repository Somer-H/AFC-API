package com.afcgym.platform.service.dto;

public record RegisterMemberCredentialsCommand(
        String pin,
        String fingerprintToken
) {}
