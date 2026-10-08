# Chat Application Backend Architecture

## Package Structure (Package-by-Feature / Domain-Driven)

```
com.chatapp
├── administration.interfaces.rest   # Admin moderation, monitoring, and audit interfaces
├── auth                             # User registration, OTP login, verification, and tokens
├── user                             # User profiles, avatar updates, and mobile search
├── conversation                     # 1-to-1 conversation sessions and chat room metadata
├── message                          # Message creation, history, and status updates (Sent/Delivered/Read)
├── media                            # Image and attachment upload & delivery
├── contact                          # Contact sync and address book management
├── websocket                        # Real-time WebSocket / STOMP channels & dispatchers
├── presence                         # Online / Offline status and typing indicator tracking
├── notification                     # Push notifications (FCM / APNs) and alerts
├── security                         # Spring Security configuration and JWT filters
├── shared                           # Common DTOs, response wrappers, and exception handlers
└── ChatApplication.java             # Main Spring Boot Application Entry Point
```
