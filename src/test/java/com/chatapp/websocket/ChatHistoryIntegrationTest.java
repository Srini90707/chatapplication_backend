package com.chatapp.websocket;

import com.chatapp.websocket.dto.ChatHistoryResponse;
import com.chatapp.websocket.dto.ChatMessageDto;
import com.chatapp.websocket.entity.ChatMessage;
import com.chatapp.websocket.repository.ChatMessageRepository;
import com.chatapp.websocket.service.MessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatHistoryIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    private static final String USER_A = "9876543210";
    private static final String USER_B = "9123456789";

    @BeforeEach
    void setUp() {
        cleanUpTestData();
        setAuthenticatedUser(USER_A);
    }

    @AfterEach
    void tearDown() {
        cleanUpTestData();
        SecurityContextHolder.clearContext();
    }

    private void cleanUpTestData() {
        jdbcTemplate.update("DELETE FROM chat_messages WHERE sender_id IN ('9876543210', '9123456789', '+919988776655', '9988776655', '+918877665544', '8877665544') OR receiver_id IN ('9876543210', '9123456789', '+919988776655', '9988776655', '+918877665544', '8877665544')");
    }

    private void setAuthenticatedUser(String username) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Acceptance criterion 4:
     * An integration test seeds a conversation with 120 messages and asserts:
     * page 0 returns 50, hasMore is true, totalElements is 120, and the returned
     * content is in ascending createdAt order.
     */
    @Test
    @DisplayName("Seeds conversation with 120 messages: page 0 returns 50, hasMore is true, totalElements is 120, ascending order")
    void shouldSeed120MessagesAndReturnPaginatedAscendingHistory() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
        List<ChatMessage> messages = new ArrayList<>(120);

        for (int i = 0; i < 120; i++) {
            // Alternate sender/receiver between USER_A and USER_B
            String sender = (i % 2 == 0) ? USER_A : USER_B;
            String receiver = (i % 2 == 0) ? USER_B : USER_A;
            String messageId = UUID.randomUUID().toString();
            LocalDateTime msgTime = baseTime.plusMinutes(i);

            jdbcTemplate.update(
                    "INSERT INTO chat_messages (id, sender_id, receiver_id, content, status, message_type, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    messageId, sender, receiver, "Message sequence #" + i, "sent", "text", msgTime
            );
        }

        // Fetch page 0 with size 50
        ChatHistoryResponse response = messageService.getChatHistory(USER_B, 0, 50);

        assertNotNull(response);
        assertEquals(50, response.getMessages().size(), "Page 0 must return 50 messages");
        assertTrue(response.isHasMore(), "hasMore must be true when 120 messages exist");
        assertEquals(120, response.getTotalElements(), "totalElements must be 120");
        assertEquals(0, response.getPage());
        assertEquals(50, response.getSize());

        // Assert that messages within page 0 are in ascending createdAt order
        List<ChatMessageDto> messageList = response.getMessages();
        for (int i = 0; i < messageList.size() - 1; i++) {
            String currentTimestamp = messageList.get(i).getTimestamp();
            String nextTimestamp = messageList.get(i + 1).getTimestamp();
            assertNotNull(currentTimestamp);
            assertNotNull(nextTimestamp);
            assertTrue(
                    currentTimestamp.compareTo(nextTimestamp) <= 0,
                    "Messages must be ordered chronologically ascending within page (oldest first)"
            );
        }

        // Verify page 0 contains the latest 50 messages (sequence #70 to #119)
        assertEquals("Message sequence #70", messageList.get(0).getText());
        assertEquals("Message sequence #119", messageList.get(49).getText());
    }

    /**
     * Acceptance criterion 5:
     * An integration test seeds messages written with +91-prefixed ids pre-migration,
     * runs the migration, and asserts the history query still finds them.
     */
    @Test
    @DisplayName("Seeds messages with +91-prefixed ids, executes migration, and asserts history query finds them")
    void shouldSeedPrefixedMessagesRunMigrationAndFindThemViaNormalisedHistoryQuery() {
        String preMigrationSender = "+919988776655";
        String preMigrationReceiver = "+918877665544";
        String messageId1 = UUID.randomUUID().toString();
        String messageId2 = UUID.randomUUID().toString();
        LocalDateTime time1 = LocalDateTime.of(2026, 2, 1, 9, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2026, 2, 1, 9, 5, 0);

        // 1. Seed raw unnormalised rows directly with SQL (pre-migration state)
        jdbcTemplate.update(
                "INSERT INTO chat_messages (id, sender_id, receiver_id, content, status, message_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                messageId1, preMigrationSender, preMigrationReceiver, "Pre-migration message 1", "sent", "text", time1
        );
        jdbcTemplate.update(
                "INSERT INTO chat_messages (id, sender_id, receiver_id, content, status, message_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                messageId2, preMigrationReceiver, preMigrationSender, "Pre-migration reply 2", "sent", "text", time2
        );

        // 2. Execute the migration script statements
        String updateSenderSql = """
                UPDATE chat_messages SET sender_id = RIGHT(regexp_replace(sender_id, '\\D', '', 'g'), 10)
                  WHERE sender_id IS NOT NULL AND sender_id <> RIGHT(regexp_replace(sender_id, '\\D', '', 'g'), 10)
                """;
        String updateReceiverSql = """
                UPDATE chat_messages SET receiver_id = RIGHT(regexp_replace(receiver_id, '\\D', '', 'g'), 10)
                  WHERE receiver_id IS NOT NULL AND receiver_id <> RIGHT(regexp_replace(receiver_id, '\\D', '', 'g'), 10)
                """;
        String indexSenderReceiverSql = """
                CREATE INDEX IF NOT EXISTS idx_chat_sender_receiver
                    ON chat_messages (sender_id, receiver_id, created_at DESC)
                """;
        String indexReceiverSenderSql = """
                CREATE INDEX IF NOT EXISTS idx_chat_receiver_sender
                    ON chat_messages (receiver_id, sender_id, created_at DESC)
                """;

        jdbcTemplate.execute(updateSenderSql);
        jdbcTemplate.execute(updateReceiverSql);
        jdbcTemplate.execute(indexSenderReceiverSql);
        jdbcTemplate.execute(indexReceiverSenderSql);

        // 3. Query chat history using +91-prefixed user id or 10-digit user id
        setAuthenticatedUser("+919988776655");
        ChatHistoryResponse response = messageService.getChatHistory("+918877665544", 0, 50);

        // 4. Assert the history query successfully finds both migrated messages
        assertNotNull(response);
        assertEquals(2, response.getMessages().size(), "Must find both migrated messages");
        assertEquals(2, response.getTotalElements());

        ChatMessageDto msg1 = response.getMessages().get(0);
        ChatMessageDto msg2 = response.getMessages().get(1);

        assertEquals("Pre-migration message 1", msg1.getText());
        assertEquals("9988776655", msg1.getSenderId(), "Sender ID must be normalised in DB");
        assertEquals("8877665544", msg1.getReceiverId(), "Receiver ID must be normalised in DB");

        assertEquals("Pre-migration reply 2", msg2.getText());
        assertEquals("8877665544", msg2.getSenderId(), "Sender ID must be normalised in DB");
        assertEquals("9988776655", msg2.getReceiverId(), "Receiver ID must be normalised in DB");
    }

    @Test
    @DisplayName("Rejects negative page index with 400 Bad Request")
    void shouldRejectNegativePageIndexWithBadRequest() {
        assertThrows(ResponseStatusException.class, () -> {
            messageService.getChatHistory(USER_B, -1, 50);
        });
    }

    @Test
    @DisplayName("Caps page size at 100 when requested size exceeds 100")
    void shouldCapPageSizeAt100() {
        ChatHistoryResponse response = messageService.getChatHistory(USER_B, 0, 200);
        assertEquals(100, response.getSize());
    }

    @Test
    @DisplayName("Defaults page size to 50 when size is 0 or negative")
    void shouldDefaultPageSizeTo50WhenNonPositive() {
        ChatHistoryResponse response = messageService.getChatHistory(USER_B, 0, 0);
        assertEquals(50, response.getSize());
    }

    @Test
    @DisplayName("Endpoint GET /api/v1/chat/history/{otherUserId} returns 200 OK with paginated payload")
    void shouldReturn200FromRestEndpoint() throws Exception {
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
        jdbcTemplate.update(
                "INSERT INTO chat_messages (id, sender_id, receiver_id, content, status, message_type, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString(), USER_A, USER_B, "Hello via REST test", "sent", "text", baseTime
        );

        mockMvc.perform(get("/api/v1/chat/history/{otherUserId}", USER_B)
                        .param("page", "0")
                        .param("size", "50")
                        .principal(() -> USER_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.messages[0].text").value("Hello via REST test"))
                .andExpect(jsonPath("$.messages[0].senderId").value(USER_A))
                .andExpect(jsonPath("$.messages[0].receiverId").value(USER_B));
    }

    @Test
    @DisplayName("Endpoint GET /api/v1/chat/history/{otherUserId} returns 400 on negative page")
    void shouldReturn400FromRestEndpointOnNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/chat/history/{otherUserId}", USER_B)
                        .param("page", "-1")
                        .param("size", "50")
                        .principal(() -> USER_A))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Persisting via sendMessage normalises raw sender and receiver IDs with prefix/whitespace")
    void shouldNormaliseIdsOnSendMessage() {
        ChatMessageDto input = ChatMessageDto.builder()
                .senderId("+91 98765 43210")
                .receiverId("+919123456789")
                .text("Testing write time normalisation")
                .messageType("text")
                .build();

        ChatMessageDto saved = messageService.sendMessage(input);

        assertNotNull(saved.getId());
        assertEquals("9876543210", saved.getSenderId());
        assertEquals("9123456789", saved.getReceiverId());

        // Verify in database
        ChatMessage inDb = chatMessageRepository.findById(saved.getId()).orElseThrow();
        assertEquals("9876543210", inDb.getSenderId());
        assertEquals("9123456789", inDb.getReceiverId());
    }
}
