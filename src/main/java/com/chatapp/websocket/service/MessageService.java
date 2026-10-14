package com.chatapp.websocket.service;

import com.chatapp.websocket.dto.ChatHistoryResponse;
import com.chatapp.websocket.dto.ChatMessageDto;

import java.util.List;

public interface MessageService {

    // Saves the incoming message and pushes it in real-time to the recipient
    ChatMessageDto sendMessage(ChatMessageDto messageDto);

    // Retrieves paginated chat history between authenticated user and other user
    ChatHistoryResponse getChatHistory(String otherUserId, int page, int size);

    // Legacy overload for backward compatibility
    List<ChatMessageDto> getChatHistory(String currentUser, String otherUser);
}
