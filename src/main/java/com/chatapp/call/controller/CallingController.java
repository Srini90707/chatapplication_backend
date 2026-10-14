package com.chatapp.call.controller;

import com.chatapp.call.dto.CallRequestDto;
import com.chatapp.call.dto.CallSignalDto;
import com.chatapp.call.service.AgoraTokenService;
import com.chatapp.notification.service.PushNotificationService;
import com.chatapp.user.entity.User;
import com.chatapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CallingController {

    private final AgoraTokenService agoraTokenService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    /**
     * REST: Caller calls this to start a call.
     * POST /call/initiate or /api/v1/call/initiate
     */
    @PostMapping(value = {"/call/initiate", "/api/v1/call/initiate"})
    public ResponseEntity<CallSignalDto> initiateCall(@RequestBody CallRequestDto request, Principal principal) {
        String callerPhone = (principal != null && principal.getName() != null)
                ? principal.getName()
                : request.getCallerId();

        if (callerPhone == null || callerPhone.trim().isEmpty()) {
            log.warn("Cannot initiate call: caller identification is missing");
            return ResponseEntity.badRequest().build();
        }

        String receiverPhone = request.getReceiverId();
        if (receiverPhone == null || receiverPhone.trim().isEmpty()) {
            log.warn("Cannot initiate call: receiver identification is missing");
            return ResponseEntity.badRequest().build();
        }

        log.info("Initiating call from [{}] to [{}] (type: {})", callerPhone, receiverPhone, request.getCallType());

        // Unique channel room for this call
        String channelName = "call_" + UUID.randomUUID().toString().substring(0, 8);

        // Generate Agora tokens for both caller and receiver
        String callerToken = agoraTokenService.generateToken(channelName, callerPhone);
        String receiverToken = agoraTokenService.generateToken(channelName, receiverPhone);

        String callerName = userRepository.findByPhoneNormalized(callerPhone)
                .map(User::getName)
                .orElse(callerPhone);

        String receiverName = userRepository.findByPhoneNormalized(receiverPhone)
                .map(User::getName)
                .orElse(receiverPhone);

        // 1. Signal receiver via WebSocket: /user/{receiverPhone}/queue/call
        CallSignalDto incomingCallSignal = CallSignalDto.builder()
                .type("CALL_OFFER")
                .callerId(callerPhone)
                .callerName(callerName)
                .receiverId(receiverPhone)
                .receiverName(receiverName)
                .channelName(channelName)
                .agoraAppId(agoraTokenService.getAppId())
                .agoraToken(receiverToken)
                .callType(request.getCallType() != null ? request.getCallType() : "VOICE")
                .build();

        // 1. Deliver to all phone number variants (with/without '+', and last 10 digits)
        // so whichever format the receiver's WebSocket session is authenticated with receives the signal
        java.util.Set<String> recipientTargets = new java.util.LinkedHashSet<>();
        recipientTargets.add(receiverPhone);
        if (receiverPhone.startsWith("+")) {
            recipientTargets.add(receiverPhone.substring(1));
        } else {
            recipientTargets.add("+" + receiverPhone);
        }
        if (receiverPhone.length() >= 10) {
            recipientTargets.add(receiverPhone.substring(receiverPhone.length() - 10));
        }

        for (String target : recipientTargets) {
            log.info("Dispatching CALL_OFFER to /user/{}/queue/call", target);
            messagingTemplate.convertAndSendToUser(target, "/queue/call", incomingCallSignal);
        }

        // 2. Also send background push notification with full Agora room & token details
        try {
            pushNotificationService.sendCallNotification(
                    callerPhone,
                    receiverPhone,
                    channelName,
                    agoraTokenService.getAppId(),
                    receiverToken
            );
        } catch (Exception e) {
            log.warn("Could not send background call push notification: {}", e.getMessage());
        }

        // Return caller's token and channel info to the caller
        CallSignalDto callerResponse = CallSignalDto.builder()
                .type("CALL_INITIATED")
                .callerId(callerPhone)
                .callerName(callerName)
                .receiverId(receiverPhone)
                .receiverName(receiverName)
                .channelName(channelName)
                .agoraAppId(agoraTokenService.getAppId())
                .agoraToken(callerToken)
                .callType(request.getCallType() != null ? request.getCallType() : "VOICE")
                .build();

        return ResponseEntity.ok(callerResponse);
    }

    /**
     * WEBSOCKET: Relays live call status updates (ANSWER, REJECT, END)
     * Sent from client to /app/call.signal
     */
    @MessageMapping("/call.signal")
    public void handleCallSignal(@Payload CallSignalDto signal, Principal principal) {
        String type = signal.getType() != null ? signal.getType().trim().toUpperCase() : "";
        log.info("Relaying call signal [{}] for channel [{}] (callerId: {}, receiverId: {})",
                type, signal.getChannelName(), signal.getCallerId(), signal.getReceiverId());

        if ("CALL_ANSWER".equals(type) || "CALL_REJECT".equals(type)) {
            // Receiver accepted or declined -> Send directly to the caller
            relaySignalToUser(signal.getCallerId(), signal);
        } else if ("CALL_END".equals(type)) {
            // One party hung up -> Relay to both parties so both screens return to IDLE
            relaySignalToUser(signal.getCallerId(), signal);
            relaySignalToUser(signal.getReceiverId(), signal);
        } else {
            // General signal fallback
            String sender = (principal != null && principal.getName() != null) ? principal.getName() : signal.getReceiverId();
            String target = sender.equals(signal.getCallerId()) ? signal.getReceiverId() : signal.getCallerId();
            relaySignalToUser(target, signal);
        }
    }

    private void relaySignalToUser(String userPhone, CallSignalDto signal) {
        if (userPhone == null || userPhone.isBlank()) {
            return;
        }

        java.util.Set<String> targetVariants = new java.util.LinkedHashSet<>();
        targetVariants.add(userPhone);
        if (userPhone.startsWith("+")) {
            targetVariants.add(userPhone.substring(1));
        } else {
            targetVariants.add("+" + userPhone);
        }
        if (userPhone.length() >= 10) {
            targetVariants.add(userPhone.substring(userPhone.length() - 10));
        }

        for (String target : targetVariants) {
            log.info("Dispatching call signal [{}] to /user/{}/queue/call", signal.getType(), target);
            messagingTemplate.convertAndSendToUser(target, "/queue/call", signal);
        }
    }
}
