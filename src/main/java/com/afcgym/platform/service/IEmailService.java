package com.afcgym.platform.service;

import com.afcgym.platform.service.dto.PasswordResetRequestedEvent;

public interface IEmailService {

    void sendPasswordReset(PasswordResetRequestedEvent event);
}
