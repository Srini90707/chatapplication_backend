package com.chatapp.user.service;

import com.chatapp.user.dto.AuthResponseDto;
import com.chatapp.user.dto.UserDto;

import java.util.List;

public interface UserService {

    AuthResponseDto registerUser(UserDto userDto);

    AuthResponseDto loginUser(UserDto userDto);

    UserDto updateUser(String number, UserDto userDto);

    UserDto getUserByNumber(String number);

    List<UserDto> getAllUsersExcept(String currentNumber);

    void updateFcmToken(String number, String token);
}
