package com.chatapp.call.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallRequestDto {
    private String callerId;
    private String receiverId; // Mobile number or user ID of the person being called
    private String callType;   // "VOICE" or "VIDEO" (defaults to "VOICE")
}
