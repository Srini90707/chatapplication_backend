package com.chatapp.websocket.mapper;

import com.chatapp.websocket.dto.ChatMessageDto;
import com.chatapp.websocket.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "text", source = "content")
    @Mapping(target = "timestamp", source = "createdAt", qualifiedByName = "formatTimestamp")
    ChatMessageDto toDto(ChatMessage message);

    @Mapping(target = "content", source = "text")
    @Mapping(target = "createdAt", ignore = true)
    ChatMessage toEntity(ChatMessageDto messageDto);

    List<ChatMessageDto> toDtoList(List<ChatMessage> messages);

    List<ChatMessage> toEntityList(List<ChatMessageDto> dtos);

    @Named("formatTimestamp")
    default String formatTimestamp(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ISO_DATE_TIME) : null;
    }
}
