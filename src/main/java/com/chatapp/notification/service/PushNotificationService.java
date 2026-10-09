package com.chatapp.notification.service;

import com.chatapp.user.entity.User;
import com.chatapp.user.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
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

    private boolean isValidFcmToken(String token) {
        return token != null && !token.isBlank() && !token.startsWith("fcm_dev_") && token.length() > 20;
    }

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
        if (!isValidFcmToken(fcmToken)) {
            log.warn("Push notification skipped: recipient {} has no valid FCM token (token: {})", receiverNumber, fcmToken);
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

            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setChannelId("default")
                            .setPriority(AndroidNotification.Priority.MAX)
                            .setDefaultSound(true)
                            .setDefaultVibrateTimings(true)
                            .setVisibility(AndroidNotification.Visibility.PUBLIC)
                            .build())
                    .build();

            Message msg = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig)
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

    /**
     * Send OTP push notification to the user's device.
     */
    public void sendOtpNotification(String phoneNumber, String otp, String clientFcmToken) {
        if (phoneNumber == null || phoneNumber.isBlank() || otp == null || otp.isBlank()) {
            return;
        }

        String targetToken = (clientFcmToken != null && !clientFcmToken.isBlank()) ? clientFcmToken : null;
        if (targetToken == null) {
            Optional<User> userOpt = userRepository.findByPhoneNormalized(phoneNumber);
            if (userOpt.isPresent() && userOpt.get().getFcmToken() != null && !userOpt.get().getFcmToken().isBlank()) {
                targetToken = userOpt.get().getFcmToken();
            }
        }

        String title = "💬 Verification Code";
        String body = "Your verification code is: " + otp + ". Valid for 5 minutes.";

        if (!isValidFcmToken(targetToken)) {
            log.info("[OTP NOTIFICATION] No valid FCM token available for {}. Console OTP: {}", phoneNumber, otp);
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            log.info("[FCM DEV SIMULATION] Push notification to: {} (Token: {}): [{}] - {}",
                    phoneNumber, targetToken, title, body);
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setChannelId("default")
                            .setPriority(AndroidNotification.Priority.MAX)
                            .setDefaultSound(true)
                            .setDefaultVibrateTimings(true)
                            .setVisibility(AndroidNotification.Visibility.PUBLIC)
                            .build())
                    .build();

            Message msg = Message.builder()
                    .setToken(targetToken)
                    .setNotification(notification)
                    .setAndroidConfig(androidConfig)
                    .putData("type", "otp")
                    .putData("otp", otp)
                    .putData("phoneNumber", phoneNumber)
                    .build();

            String response = FirebaseMessaging.getInstance().send(msg);
            log.info("FCM OTP push notification sent to {} successfully: {}", phoneNumber, response);
        } catch (Exception e) {
            log.error("Failed to send FCM OTP notification to {}: {}", phoneNumber, e.getMessage());
        }
    }
}
