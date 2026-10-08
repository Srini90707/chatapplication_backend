package com.chatapp.security.otp.service;

import com.chatapp.security.otp.dto.OtpDto;

public interface OtpService {

    OtpDto generateOtp(String number);

    boolean verifyOtp(String number, String otp);

    
}
