package com.chatapp.websocket.repository;

import com.chatapp.websocket.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("""
        SELECT m FROM ChatMessage m
        WHERE (m.senderId = :user1 AND m.receiverId = :user2)
           OR (m.senderId = :user2 AND m.receiverId = :user1)
    """)
    Page<ChatMessage> findChatHistory(@Param("user1") String user1,
                                      @Param("user2") String user2,
                                      Pageable pageable);
}
