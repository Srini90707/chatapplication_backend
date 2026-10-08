# Chat Application — Enterprise Java Backend 🚀

Production-grade, high-performance Java backend built with **Spring Boot 3.3**, **Java 21**, **Spring Security 6 (Stateless JWT)**, **WebSocket (STOMP)**, **Spring Data JPA**, **PostgreSQL 16**, and **Redis 7**.

---

## 🏗️ Architecture & Package Layout

```
Backend/
├── .gitignore                          # Standard git ignore for Java/Maven/IntelliJ/VSCode
├── Dockerfile                          # Multi-stage containerization build (JDK 21 -> JRE 21 alpine)
├── docker-compose.yml                  # Production-like multi-container setup (PostgreSQL, Redis, App)
├── pom.xml                             # Maven build configuration with all dependencies
├── README.md                           # Comprehensive documentation and developer guide
├── src/
│   ├── main/
│   │   ├── java/com/chatapp/backend/
│   │   │   ├── ChatBackendApplication.java       # Spring Boot main bootstrap class
│   │   │   │
│   │   │   ├── common/                           # Cross-cutting constants and enums
│   │   │   │   ├── constants/
│   │   │   │   │   └── AppConstants.java         # Global constants, Redis keys, pagination defaults
│   │   │   │   └── enums/
│   │   │   │       ├── MessageStatus.java        # sending, sent, delivered, read, failed
│   │   │   │       └── MessageType.java          # TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT
│   │   │   │
│   │   │   ├── config/                           # Application configuration beans
│   │   │   │   ├── CorsConfig.java               # Cross-Origin Resource Sharing policy
│   │   │   │   ├── OpenApiConfig.java            # Swagger / OpenAPI 3 specifications & JWT scheme
│   │   │   │   ├── RedisConfig.java              # Distributed caching & presence tracking
│   │   │   │   ├── SecurityConfig.java           # Spring Security 6 stateless filter chain
│   │   │   │   └── WebSocketConfig.java          # STOMP broker, message channels & SockJS endpoints
│   │   │   │
│   │   │   ├── controller/                       # REST API Web Layer
│   │   │   │   ├── AuthController.java           # Phone OTP registration, login, refresh, logout
│   │   │   │   ├── ConversationController.java   # Active conversations, inbox, last messages
│   │   │   │   ├── MessageController.java        # Message history, send message, mark as read
│   │   │   │   └── UserController.java           # Contacts directory, user profiles, current user
│   │   │   │
│   │   │   ├── dto/                              # Data Transfer Objects & Validation rules
│   │   │   │   ├── request/
│   │   │   │   │   ├── auth/                     # RegisterRequest, LoginRequest, VerifyOtpRequest, RefreshTokenRequest
│   │   │   │   │   ├── chat/                     # SendMessageRequest, UpdateMessageStatusRequest, TypingNotificationRequest
│   │   │   │   │   └── user/                     # UpdateProfileRequest
│   │   │   │   └── response/
│   │   │   │       ├── ApiResponse.java          # Generic standard response wrapper
│   │   │   │       ├── auth/                     # AuthResponse, SendOtpResponse
│   │   │   │       ├── chat/                     # MessageDto, ConversationDto, TypingEventDto, UserStatusEventDto
│   │   │   │       └── user/                     # UserDto
│   │   │   │
│   │   │   ├── entity/                           # JPA Database Entities
│   │   │   │   ├── BaseEntity.java               # MappedSuperclass with UUID, createdAt, updatedAt auditing
│   │   │   │   ├── ConversationEntity.java       # Conversations (Direct & Group)
│   │   │   │   ├── ConversationParticipantEntity.java # User membership, unread badges
│   │   │   │   ├── MessageEntity.java            # Chat messages, sender, receiver, status, payloads
│   │   │   │   ├── MessageReceiptEntity.java     # Per-user delivery and read receipts
│   │   │   │   ├── RefreshTokenEntity.java       # Revocable refresh tokens for secure JWT rotation
│   │   │   │   └── UserEntity.java               # Users table, profile info, online status
│   │   │   │
│   │   │   ├── exception/                        # Exception handling & RFC 7807 responses
│   │   │   │   ├── AppException.java             # Base business logic exception
│   │   │   │   ├── ErrorResponse.java            # Standardized error payload
│   │   │   │   ├── GlobalExceptionHandler.java   # Centralized @RestControllerAdvice
│   │   │   │   ├── InvalidOtpException.java      # 400 Bad Request OTP verification error
│   │   │   │   ├── ResourceNotFoundException.java# 404 Not Found error
│   │   │   │   └── UnauthorizedException.java    # 401 Unauthorized error
│   │   │   │
│   │   │   ├── mapper/                           # DTO <-> Entity mappers
│   │   │   │   ├── ConversationMapper.java
│   │   │   │   ├── MessageMapper.java
│   │   │   │   └── UserMapper.java
│   │   │   │
│   │   │   ├── repository/                       # Spring Data JPA Repositories
│   │   │   │   ├── ConversationParticipantRepository.java
│   │   │   │   ├── ConversationRepository.java
│   │   │   │   ├── MessageReceiptRepository.java
│   │   │   │   ├── MessageRepository.java
│   │   │   │   ├── RefreshTokenRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   │
│   │   │   ├── security/                         # JWT & Authentication Filter Engine
│   │   │   │   ├── CustomUserDetailsService.java # Loads user principal from DB
│   │   │   │   ├── JwtAuthenticationEntryPoint.java # 401 unauthorized handler
│   │   │   │   ├── JwtAuthenticationFilter.java  # Bearer token validation per request
│   │   │   │   ├── JwtTokenProvider.java         # Token generation, claims parsing, validation
│   │   │   │   └── UserPrincipal.java            # Spring Security UserDetails adapter
│   │   │   │
│   │   │   ├── service/                          # Business Services & Implementations
│   │   │   │   ├── AuthService.java              # & impl/AuthServiceImpl.java
│   │   │   │   ├── ConversationService.java      # & impl/ConversationServiceImpl.java
│   │   │   │   ├── MessageService.java           # & impl/MessageServiceImpl.java
│   │   │   │   ├── OtpService.java               # & impl/OtpServiceImpl.java
│   │   │   │   ├── PresenceService.java          # & impl/PresenceServiceImpl.java
│   │   │   │   └── UserService.java              # & impl/UserServiceImpl.java
│   │   │   │
│   │   │   └── websocket/                        # STOMP WebSocket Messaging Layer
│   │   │       ├── WebSocketAuthInterceptor.java # JWT authentication in STOMP CONNECT header
│   │   │       ├── WebSocketChatController.java  # Destination routes: /app/chat.send, /app/chat.typing
│   │   │       └── WebSocketEventListener.java   # Presence event listeners (connect & disconnect)
│   │   │
│   │   └── resources/
│   │       ├── application.yml                   # Base configuration
│   │       ├── application-dev.yml               # Local development profile (Postgres + Redis)
│   │       ├── application-prod.yml              # Production profile with environment variables
│   │       ├── logback-spring.xml                # Structured rolling logging config
│   │       └── db/migration/
│   │           └── V1__init_schema.sql           # Flyway DDL migration script
│   │
│   └── test/
│       ├── java/com/chatapp/backend/
│       │   └── ChatBackendApplicationTests.java  # Context integration test
│       └── resources/
│           └── application-test.yml              # H2 in-memory test configuration
```

---

## ⚡ Quick Start

### 1. Run with Docker Compose (PostgreSQL + Redis + Backend)
```bash
docker compose up -d
```

### 2. Run Locally via Maven
Ensure you have Java 21 and Maven installed. Start the database dependencies:
```bash
docker compose up -d postgres redis
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend server starts on `http://localhost:8080`.

---

## 🔌 API & WebSocket Documentation

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3 JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### STOMP WebSocket Protocol:
- **Endpoint**: `ws://localhost:8080/ws` (Native WebSocket) or `http://localhost:8080/ws` (SockJS)
- **CONNECT Header**: `Authorization: Bearer <jwt_token>`
- **User Message Queue**: `/user/queue/messages`
- **User Typing Queue**: `/user/queue/typing`
- **User Status Queue**: `/user/queue/message-status`
- **Global Presence Topic**: `/topic/status`
- **Send Message Destination**: `/app/chat.send`
- **Send Typing Status Destination**: `/app/chat.typing`
