package com.afcgym.platform.service;

public interface IPasswordResetService {

    void requestReset(String email);

    void confirmReset(String token, String newPassword);
}
