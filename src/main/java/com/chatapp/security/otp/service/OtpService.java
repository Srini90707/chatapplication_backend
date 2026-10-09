package com.chatapp.security.otp.service;

import com.chatapp.security.otp.dto.OtpDto;

public interface OtpService {

    OtpDto generateOtp(String number);

    OtpDto generateOtp(String number, String fcmToken);

    boolean verifyOtp(String number, String otp);

    
}
