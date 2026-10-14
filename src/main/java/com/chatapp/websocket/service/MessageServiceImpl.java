package com.chatapp.websocket.service;

import com.chatapp.chat.util.UserIdNormaliser;
import com.chatapp.websocket.dto.ChatHistoryResponse;
import com.chatapp.websocket.dto.ChatMessageDto;
import com.chatapp.websocket.entity.ChatMessage;
import com.chatapp.websocket.mapper.ChatMessageMapper;
import com.chatapp.websocket.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.chatapp.notification.service.PushNotificationService pushNotificationService;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public ChatMessageDto sendMessage(ChatMessageDto dto) {
        String type = (dto.getMessageType() != null && !dto.getMessageType().isBlank())
                ? dto.getMessageType()
                : "text";
        String content = dto.getText() != null ? dto.getText() : "";

        LocalDateTime now = LocalDateTime.now();

        // 1. Normalise sender and receiver IDs before persisting
        String normalisedSenderId = UserIdNormaliser.normalise(dto.getSenderId());
        String normalisedReceiverId = UserIdNormaliser.normalise(dto.getReceiverId());

        ChatMessage entity = ChatMessage.builder()
                .senderId(normalisedSenderId)
                .receiverId(normalisedReceiverId)
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
        String receiver = saved.getReceiverId() != null ? saved.getReceiverId().trim() : "";
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

        log.info("Message [{}] successfully saved and dispatched", saved.getId());
        log.debug("Message [{}] details: roomId={}, room10={}, receiver={}", saved.getId(), roomId, room10, receiver);

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
    public ChatHistoryResponse getChatHistory(String otherUserId, int page, int size) {
        // 1. Resolve the current user from SecurityContextHolder, never from a request parameter
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated");
        }
        String currentUserId = authentication.getName();

        // 2. Reject negative page with a 400
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page index must not be negative");
        }

        // 3. Cap size at 100; default 50
        if (size <= 0) {
            size = 50;
        } else if (size > 100) {
            size = 100;
        }

        // 4. Normalise both IDs before querying
        String user1 = UserIdNormaliser.normalise(currentUserId);
        String user2 = UserIdNormaliser.normalise(otherUserId);

        // 5. Query with PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messagePage = messageRepository.findChatHistory(user1, user2, pageable);

        // 6. Reverse the returned content to ascending order before putting it in response
        List<ChatMessage> entities = new ArrayList<>(messagePage.getContent());
        Collections.reverse(entities);

        // 7. Map entities to DTOs with MapStruct mapper
        List<ChatMessageDto> messageDtos = chatMessageMapper.toDtoList(entities);

        return ChatHistoryResponse.builder()
                .messages(messageDtos)
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .hasMore(messagePage.hasNext())
                .build();
    }

    @Override
    public List<ChatMessageDto> getChatHistory(String currentUser, String otherUser) {
        String u1 = UserIdNormaliser.normalise(currentUser);
        String u2 = UserIdNormaliser.normalise(otherUser);
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ChatMessage> pageResult = messageRepository.findChatHistory(u1, u2, pageable);
        return chatMessageMapper.toDtoList(pageResult.getContent());
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
