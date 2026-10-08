package com.chatapp.security.otp.repository;

import com.chatapp.security.otp.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, String> {



    Optional<Otp> findByNumberAndOtp(String number, String otp);
}
