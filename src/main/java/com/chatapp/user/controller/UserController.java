package com.chatapp.user.controller;

import com.chatapp.user.dto.AuthResponseDto;
import com.chatapp.user.dto.UserDto;
import com.chatapp.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> registerUser(@Valid @RequestBody UserDto userDto) {
        AuthResponseDto response = userService.registerUser(userDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody UserDto userDto) {
        AuthResponseDto response = userService.loginUser(userDto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("update/{number}")
    public ResponseEntity<UserDto> updateUser(@PathVariable("number") String number, @RequestBody UserDto userDto) {
        UserDto updatedUser = userService.updateUser(number, userDto);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("retrive/{number}")
    public ResponseEntity<UserDto> getUserByNumber(@PathVariable("number") String number) {
        UserDto user = userService.getUserByNumber(number);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    public ResponseEntity<UserDto> searchUser(@RequestParam("mobile") String mobile) {
        UserDto user = userService.getUserByNumber(mobile);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/all")
    public ResponseEntity<java.util.List<UserDto>> getAllUsers(
            @RequestParam(value = "excludeNumber", required = false) String excludeNumber,
            java.security.Principal principal
    ) {
        String currentNumber = excludeNumber;
        if ((currentNumber == null || currentNumber.trim().isEmpty()) && principal != null) {
            currentNumber = principal.getName();
        }
        java.util.List<UserDto> users = userService.getAllUsersExcept(currentNumber);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<java.util.Map<String, String>> updateFcmToken(
            @RequestBody java.util.Map<String, String> payload,
            java.security.Principal principal
    ) {
        String number = payload.get("number");
        if ((number == null || number.isBlank()) && principal != null) {
            number = principal.getName();
        }
        String token = payload.get("fcmToken");
        if (number != null && token != null) {
            userService.updateFcmToken(number, token);
            return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "FCM token updated"));
        }
        return ResponseEntity.badRequest().body(java.util.Map.of("status", "error", "message", "number and fcmToken required"));
    }
}
