package com.chatapp.call.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallSignalDto {
    // "CALL_OFFER", "CALL_ANSWER", "CALL_REJECT", "CALL_END", "CALL_BUSY"
    private String type;

    private String callerId;
    private String callerName;

    private String receiverId;
    private String receiverName;

    private String channelName;
    private String agoraAppId;
    private String agoraToken;

    @Builder.Default
    private String callType = "VOICE";
}
