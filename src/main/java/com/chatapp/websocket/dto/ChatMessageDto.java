package com.chatapp.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {

    private String id;              // Unique message ID (UUID or generated on client)
    private String senderId;        // Mobile number of the sender (e.g. "+917672010079")
    private String receiverId;      // Mobile number of the recipient (e.g. "+919494952361")
    private String text;            // Message text body
    private String timestamp;       // ISO timestamp string
    private String status;          // "sending", "sent", "delivered", "read"
    private String messageType;     // "text", "image", "pdf"
    private String attachmentUrl;   // URL to download/view the uploaded media
    private String attachmentName;  // Original file name (e.g. document.pdf)
    private Long attachmentSize;    // File size in bytes
}
