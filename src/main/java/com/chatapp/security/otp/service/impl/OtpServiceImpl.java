package com.chatapp.security.otp.service.impl;

import com.chatapp.security.otp.dto.OtpDto;
import com.chatapp.security.otp.entity.Otp;
import com.chatapp.security.otp.mapper.OtpMapper;
import com.chatapp.security.otp.repository.OtpRepository;
import com.chatapp.security.otp.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final OtpMapper otpMapper;
    private final com.chatapp.notification.service.PushNotificationService pushNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public OtpDto generateOtp(String number) {
        return generateOtp(number, null);
    }

    @Override
    public OtpDto generateOtp(String number, String fcmToken) {
        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        String normalizedNumber = number.trim();
        String otpValue = String.format("%06d", secureRandom.nextInt(1000000));

        log.info("========================================");
        log.info(">> OTP for {}: {} <<", normalizedNumber, otpValue);
        log.info("========================================");
        System.out.println(">> OTP for " + normalizedNumber + ": " + otpValue);

        Otp otpEntity = Otp.builder()
                .number(normalizedNumber)
                .otp(otpValue)
                .build();

        Otp savedOtp = otpRepository.save(otpEntity);

        // Dispatch push notification to user's device
        try {
            pushNotificationService.sendOtpNotification(normalizedNumber, otpValue, fcmToken);
        } catch (Exception e) {
            log.warn("Failed to dispatch OTP push notification: {}", e.getMessage());
        }

        return otpMapper.toDto(savedOtp);
    }

    @Override
    public boolean verifyOtp(String number, String otp) {
        if (number == null || otp == null || number.trim().isEmpty() || otp.trim().isEmpty()) {
            return false;
        }

        String normalizedNumber = number.trim();
        String normalizedOtp = otp.trim();

        Optional<Otp> otpOptional = otpRepository.findByNumberAndOtp(normalizedNumber, normalizedOtp);
        if (otpOptional.isPresent()) {
            otpRepository.delete(otpOptional.get());
            return true;
        }

        return false;
    }

   
}
