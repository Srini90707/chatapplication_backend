package com.chatapp.user.controller;

import com.chatapp.user.dto.UserDto;
import com.chatapp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
public class ChatController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getChats(
            @RequestParam(value = "excludeNumber", required = false) String excludeNumber,
            Principal principal
    ) {
        String currentNumber = excludeNumber;
        if ((currentNumber == null || currentNumber.trim().isEmpty()) && principal != null) {
            currentNumber = principal.getName();
        }
        List<UserDto> users = userService.getAllUsersExcept(currentNumber);
        return ResponseEntity.ok(users);
    }
}
