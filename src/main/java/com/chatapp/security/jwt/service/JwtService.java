package com.chatapp.security.jwt.service;

import com.chatapp.security.jwt.dto.JwtResponseDto;

public interface JwtService {

    String generateAccessToken(String number);

    String generateRefreshToken(String number);

    JwtResponseDto createTokens(String number);

    boolean validateToken(String token);

    String getNumberFromToken(String token);
}
