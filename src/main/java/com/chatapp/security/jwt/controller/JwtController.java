package com.chatapp.security.jwt.controller;

import com.chatapp.security.jwt.dto.JwtResponseDto;
import com.chatapp.security.jwt.dto.RefreshTokenRequestDto;
import com.chatapp.security.jwt.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jwt")
@RequiredArgsConstructor
public class JwtController {

    private final JwtService jwtService;

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponseDto> refreshToken(@RequestBody RefreshTokenRequestDto requestDto) {
        if (requestDto.getRefreshToken() == null || !jwtService.validateToken(requestDto.getRefreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String number = jwtService.getNumberFromToken(requestDto.getRefreshToken());
        JwtResponseDto responseDto = jwtService.createTokens(number);
        return ResponseEntity.ok(responseDto);
    }
}
