package com.chatapp.security.otp.mapper;

import com.chatapp.security.otp.dto.OtpDto;
import com.chatapp.security.otp.entity.Otp;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OtpMapper {

    OtpDto toDto(Otp otp);

    Otp toEntity(OtpDto otpDto);

  
}
