package com.chatapp.security.otp.controller;

import com.chatapp.security.otp.dto.OtpDto;
import com.chatapp.security.otp.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/generate/{number}")
    public ResponseEntity<OtpDto> generateOtp(@PathVariable("number") String number) {
        OtpDto otpDto = otpService.generateOtp(number);
        return new ResponseEntity<>(otpDto, HttpStatus.CREATED);
    }

    @PostMapping("/verify")
    public ResponseEntity<Boolean> verifyOtp(@RequestBody OtpDto otpDto) {
        boolean isValid = otpService.verifyOtp(otpDto.getNumber(), otpDto.getOtp());
        return ResponseEntity.ok(isValid);
    }
}
