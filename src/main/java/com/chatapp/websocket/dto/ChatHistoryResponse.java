package com.chatapp.websocket.dto;

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
