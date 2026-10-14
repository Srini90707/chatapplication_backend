package com.chatapp.chat.dto;

import com.chatapp.websocket.dto.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {
    private List<ChatMessageDto> messages;
    private int page;
    private int size;
    private long totalElements;
    private boolean hasMore;
}
