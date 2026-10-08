package com.chatapp.notification.service;

import com.chatapp.user.entity.User;
import com.chatapp.user.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserRepository userRepository;

    /**
     * Send push notification for a chat message to the recipient's registered device.
     */
    public void sendChatNotification(String senderNumber, String receiverNumber, String messageText, String messageType) {
        if (receiverNumber == null || receiverNumber.isBlank()) {
            return;
        }

        // 1. Look up recipient to find their FCM token
        Optional<User> recipientOpt = userRepository.findByPhoneNormalized(receiverNumber);
        if (recipientOpt.isEmpty()) {
            log.debug("Push notification skipped: recipient not found for number {}", receiverNumber);
            return;
        }

        User recipient = recipientOpt.get();
        String fcmToken = recipient.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("Push notification skipped: recipient {} has no registered FCM token", receiverNumber);
            return;
        }

        // 2. Look up sender to show sender's display name
        String senderTitle = senderNumber;
        Optional<User> senderOpt = userRepository.findByPhoneNormalized(senderNumber);
        if (senderOpt.isPresent() && senderOpt.get().getName() != null && !senderOpt.get().getName().isBlank()) {
            senderTitle = senderOpt.get().getName();
        }

        // 3. Prepare notification body
        String notificationBody;
        if ("image".equalsIgnoreCase(messageType)) {
            notificationBody = (messageText != null && !messageText.isBlank()) 
                    ? "📷 " + messageText 
                    : "📷 Photo";
        } else if ("pdf".equalsIgnoreCase(messageType) || "document".equalsIgnoreCase(messageType)) {
            notificationBody = (messageText != null && !messageText.isBlank()) 
                    ? "📄 " + messageText 
                    : "📄 Document";
        } else {
            notificationBody = (messageText != null && !messageText.isBlank()) 
                    ? messageText 
                    : "New message";
        }

        // 4. Send notification or simulate in dev mode
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("[FCM DEV SIMULATION] Push notification to: {} (Token: {}): [{}] - {}",
                    receiverNumber, fcmToken, senderTitle, notificationBody);
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(senderTitle)
                    .setBody(notificationBody)
                    .build();

            Message msg = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .putData("senderId", senderNumber)
                    .putData("receiverId", receiverNumber)
                    .putData("messageType", messageType != null ? messageType : "text")
                    .build();

            String response = FirebaseMessaging.getInstance().send(msg);
            log.info("FCM push notification sent to {} successfully: {}", receiverNumber, response);

        } catch (Exception e) {
            log.error("Failed to send FCM push notification to {}: {}", receiverNumber, e.getMessage());
        }
    }
}
