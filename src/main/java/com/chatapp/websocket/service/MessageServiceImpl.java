package com.chatapp.websocket.service;

import com.chatapp.websocket.dto.ChatMessageDto;
import com.chatapp.websocket.entity.ChatMessage;
import com.chatapp.websocket.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.chatapp.notification.service.PushNotificationService pushNotificationService;

    @Override
    @Transactional
    public ChatMessageDto sendMessage(ChatMessageDto dto) {
        String type = (dto.getMessageType() != null && !dto.getMessageType().isBlank())
                ? dto.getMessageType()
                : "text";
        String content = dto.getText() != null ? dto.getText() : "";

        LocalDateTime now = LocalDateTime.now();

        // 1. Save message to PostgreSQL
        ChatMessage entity = ChatMessage.builder()
                .senderId(dto.getSenderId())
                .receiverId(dto.getReceiverId())
                .content(content)
                .status("sent")
                .messageType(type)
                .attachmentUrl(dto.getAttachmentUrl())
                .attachmentName(dto.getAttachmentName())
                .attachmentSize(dto.getAttachmentSize())
                .createdAt(now)
                .build();

        ChatMessage saved = messageRepository.save(entity);
        LocalDateTime createdTime = saved.getCreatedAt() != null ? saved.getCreatedAt() : now;

        // 2. Prepare the payload to broadcast
        ChatMessageDto responseDto = ChatMessageDto.builder()
                .id(saved.getId())
                .senderId(saved.getSenderId())
                .receiverId(saved.getReceiverId())
                .text(saved.getContent())
                .status(saved.getStatus())
                .messageType(saved.getMessageType())
                .attachmentUrl(saved.getAttachmentUrl())
                .attachmentName(saved.getAttachmentName())
                .attachmentSize(saved.getAttachmentSize())
                .timestamp(createdTime.format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

        // 3a. Push in real-time to the recipient's private queue: /user/{receiverId}/queue/messages
        String receiver = saved.getReceiverId().trim();
        messagingTemplate.convertAndSendToUser(
                receiver,
                "/queue/messages",
                responseDto
        );

        // Also send to alternate formatting (with or without '+') for compatibility
        if (receiver.startsWith("+")) {
            messagingTemplate.convertAndSendToUser(receiver.substring(1), "/queue/messages", responseDto);
        } else {
            messagingTemplate.convertAndSendToUser("+" + receiver, "/queue/messages", responseDto);
        }

        // 3b. Broadcast to the shared conversation room topic: /topic/chat.{roomId}
        String roomId = getRoomId(saved.getSenderId(), saved.getReceiverId());
        messagingTemplate.convertAndSend("/topic/chat." + roomId, responseDto);

        // Also broadcast to 10-digit room topic in case client connected with 10 digits
        String room10 = getRoomId10(saved.getSenderId(), saved.getReceiverId());
        if (!room10.equals(roomId)) {
            messagingTemplate.convertAndSend("/topic/chat." + room10, responseDto);
        }

        log.info("Message [{}] saved and broadcast to rooms [/topic/chat.{}, /topic/chat.{}] & user [{}]", 
                saved.getId(), roomId, room10, receiver);

        // 4. Send Firebase push notification to recipient's device
        try {
            pushNotificationService.sendChatNotification(
                    saved.getSenderId(),
                    saved.getReceiverId(),
                    saved.getContent(),
                    saved.getMessageType()
            );
        } catch (Exception e) {
            log.warn("Push notification dispatch failed: {}", e.getMessage());
        }

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatHistory(String currentUser, String otherUser) {
        List<ChatMessage> history = messageRepository.findChatHistory(currentUser, otherUser);

        return history.stream()
                .map(msg -> ChatMessageDto.builder()
                        .id(msg.getId())
                        .senderId(msg.getSenderId())
                        .receiverId(msg.getReceiverId())
                        .text(msg.getContent())
                        .status(msg.getStatus())
                        .messageType(msg.getMessageType())
                        .attachmentUrl(msg.getAttachmentUrl())
                        .attachmentName(msg.getAttachmentName())
                        .attachmentSize(msg.getAttachmentSize())
                        .timestamp(msg.getCreatedAt() != null 
                                ? msg.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME) 
                                : null)
                        .build())
                .toList();
    }

    private String getRoomId(String user1, String user2) {
        String u1 = (user1 != null ? user1 : "").replaceAll("\\D", "");
        String u2 = (user2 != null ? user2 : "").replaceAll("\\D", "");
        if (u1.compareTo(u2) < 0) {
            return u1 + "_" + u2;
        } else {
            return u2 + "_" + u1;
        }
    }

    private String getRoomId10(String user1, String user2) {
        String u1 = to10Digits(user1);
        String u2 = to10Digits(user2);
        if (u1.compareTo(u2) < 0) {
            return u1 + "_" + u2;
        } else {
            return u2 + "_" + u1;
        }
    }

    private String to10Digits(String val) {
        if (val == null) return "";
        String d = val.replaceAll("\\D", "");
        return d.length() > 10 ? d.substring(d.length() - 10) : d;
    }
}
