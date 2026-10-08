package com.chatapp.user.service.impl;

import com.chatapp.security.jwt.dto.JwtResponseDto;
import com.chatapp.security.jwt.service.JwtService;
import com.chatapp.user.dto.AuthResponseDto;
import com.chatapp.user.dto.UserDto;
import com.chatapp.user.entity.User;
import com.chatapp.user.exception.UserAlreadyExistsException;
import com.chatapp.user.exception.UserNotFoundException;
import com.chatapp.user.mapper.UserMapper;
import com.chatapp.user.repository.UserRepository;
import com.chatapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    @Override
    public AuthResponseDto registerUser(UserDto userDto) {
        if (userDto.getNumber() == null || userDto.getNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("User phone number cannot be null or empty");
        }

        String normalizedNumber = userDto.getNumber().trim();
        if (userRepository.existsByNumber(normalizedNumber)) {
            throw new UserAlreadyExistsException("User already exists with number: " + normalizedNumber);
        }

        User user = userMapper.toEntity(userDto);
        user.setNumber(normalizedNumber);
        if (userDto.getFcmToken() != null && !userDto.getFcmToken().isBlank()) {
            user.setFcmToken(userDto.getFcmToken().trim());
        }
        User savedUser = userRepository.save(user);

        UserDto userResponse = userMapper.toDto(savedUser);
        JwtResponseDto tokens = jwtService.createTokens(normalizedNumber);

        return AuthResponseDto.builder()
                .user(userResponse)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .tokenType(tokens.getTokenType())
                .build();
    }

    @Override
    public AuthResponseDto loginUser(UserDto userDto) {
        if (userDto.getNumber() == null || userDto.getNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("User phone number cannot be null or empty");
        }

        String normalizedNumber = userDto.getNumber().trim();
        User user = userRepository.findByNumber(normalizedNumber)
                .orElseThrow(() -> new UserNotFoundException("User not found with number: " + normalizedNumber));

        if (userDto.getFcmToken() != null && !userDto.getFcmToken().isBlank()) {
            user.setFcmToken(userDto.getFcmToken().trim());
            userRepository.save(user);
        }

        UserDto userResponse = userMapper.toDto(user);
        JwtResponseDto tokens = jwtService.createTokens(normalizedNumber);

        return AuthResponseDto.builder()
                .user(userResponse)
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .tokenType(tokens.getTokenType())
                .build();
    }

    @Override
    public UserDto updateUser(String number, UserDto userDto) {
        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("User phone number cannot be null or empty");
        }

        String normalizedNumber = number.trim();
        User existingUser = userRepository.findByNumber(normalizedNumber)
                .orElseThrow(() -> new UserNotFoundException("User not found with number: " + normalizedNumber));

        if (userDto.getName() != null && !userDto.getName().trim().isEmpty()) {
            existingUser.setName(userDto.getName().trim());
        }

        User updatedUser = userRepository.save(existingUser);
        return userMapper.toDto(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByNumber(String number) {
        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("User phone number cannot be null or empty");
        }

        String normalizedNumber = number.trim();
        User user = userRepository.findByNumber(normalizedNumber)
                .orElseThrow(() -> new UserNotFoundException("User not found with number: " + normalizedNumber));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<UserDto> getAllUsersExcept(String currentNumber) {
        java.util.List<User> users;
        if (currentNumber != null && !currentNumber.trim().isEmpty()) {
            users = userRepository.findByNumberNot(currentNumber.trim());
        } else {
            users = userRepository.findAll();
        }
        return userMapper.toDtoList(users);
    }

    @Override
    public void updateFcmToken(String number, String token) {
        if (number == null || number.trim().isEmpty() || token == null || token.trim().isEmpty()) {
            return;
        }
        String cleanNumber = number.trim();
        java.util.Optional<User> userOpt = userRepository.findByPhoneNormalized(cleanNumber);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByNumber(cleanNumber);
        }
        if (userOpt.isEmpty()) {
            String digits = cleanNumber.replaceAll("[^0-9]", "");
            if (digits.length() >= 10) {
                userOpt = userRepository.findByPhoneNormalized(digits.substring(digits.length() - 10));
            }
        }
        userOpt.ifPresent(user -> {
            user.setFcmToken(token.trim());
            userRepository.save(user);
        });
    }
}
