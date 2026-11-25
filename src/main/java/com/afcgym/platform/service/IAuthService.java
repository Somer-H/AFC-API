package com.afcgym.platform.service;

import com.afcgym.platform.service.dto.LoginResult;
import org.springframework.security.oauth2.jwt.Jwt;

public interface IAuthService {

    LoginResult login(String username, String password);

    LoginResult refresh(Jwt token);

    void logout(Jwt token);
}
