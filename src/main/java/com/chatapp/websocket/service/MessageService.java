package com.chatapp.websocket.service;

import com.chatapp.websocket.dto.ChatMessageDto;

import java.util.List;

public interface MessageService {

    // Saves the incoming message and pushes it in real-time to the recipient
    ChatMessageDto sendMessage(ChatMessageDto messageDto);

    // Retrieves chat history between two users in chronological order
    List<ChatMessageDto> getChatHistory(String currentUser, String otherUser);
}
