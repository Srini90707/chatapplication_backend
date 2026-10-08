package com.chatapp.websocket.controller;

import com.chatapp.websocket.dto.ChatMessageDto;
import com.chatapp.websocket.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * WEBSOCKET: Receives live messages sent to /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void handleIncomingMessage(@Payload ChatMessageDto messageDto, Principal principal) {
        // If senderId was omitted, use the authenticated WebSocket user
        if (principal != null && (messageDto.getSenderId() == null || messageDto.getSenderId().isBlank())) {
            messageDto.setSenderId(principal.getName());
        }

        messageService.sendMessage(messageDto);
    }

    /**
     * WEBSOCKET: Real-time typing indicators sent to /app/chat.typing
     * Payload: { "receiverId": "+91...", "isTyping": true }
     */
    @MessageMapping("/chat.typing")
    public void handleTypingIndicator(@Payload Map<String, Object> payload, Principal principal) {
        String receiverId = (String) payload.get("receiverId");
        Boolean isTyping = (Boolean) payload.get("isTyping");
        String senderId = (principal != null) ? principal.getName() : (String) payload.get("senderId");

        if (receiverId != null && senderId != null) {
            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/typing",
                    Map.of("userId", senderId, "isTyping", isTyping != null && isTyping)
            );
        }
    }

    /**
     * REST API: Fetches message history when a user opens a conversation screen.
     * GET /messages/{contactNumber}
     */
    @GetMapping("/messages/{contactNumber}")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable("contactNumber") String contactNumber,
            @RequestParam(value = "currentUser", required = false) String currentUser,
            Principal principal
    ) {
        String activeUser = currentUser;
        if ((activeUser == null || activeUser.trim().isEmpty()) && principal != null) {
            activeUser = principal.getName();
        }

        String c1 = activeUser != null ? activeUser.trim() : null;
        String c2 = contactNumber != null ? contactNumber.trim() : null;

        List<ChatMessageDto> messages = messageService.getChatHistory(c1, c2);
        return ResponseEntity.ok(messages);
    }

        /**
     * REST API: Sends a message (saves to DB and pushes via WebSocket to recipient)
     * POST /messages
     */
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageDto> postMessage(
            @RequestBody ChatMessageDto messageDto,
            Principal principal
    ) {
        if (principal != null && (messageDto.getSenderId() == null || messageDto.getSenderId().isBlank())) {
            messageDto.setSenderId(principal.getName());
        }

        ChatMessageDto savedMessage = messageService.sendMessage(messageDto);
        return ResponseEntity.ok(savedMessage);
    }

}
