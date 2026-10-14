package com.chatapp.call.service;

import io.agora.media.RtcTokenBuilder;
import io.agora.media.RtcTokenBuilder.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AgoraTokenService {

    @Value("${app.agora.app-id}")
    private String appId;

    @Value("${app.agora.app-certificate}")
    private String appCertificate;

    // Token validity in seconds (1 hour)
    private static final int TOKEN_EXPIRATION_IN_SECONDS = 3600;

    /**
     * Generates an Agora RTC token for a specific voice channel and user.
     *
     * @param channelName The unique room name (e.g., "call_9876543210_9123456789")
     * @param userAccount The user's ID or phone number
     * @return The signed Agora token string
     */
    public String generateToken(String channelName, String userAccount) {
        if (appId == null || appId.isBlank() || appCertificate == null || appCertificate.isBlank()) {
            throw excitingIllegalStateException("Agora App ID and Certificate must be configured in application.yml");
        }

        RtcTokenBuilder tokenBuilder = new RtcTokenBuilder();

        // Expiration timestamp in seconds
        int timestamp = (int) (System.currentTimeMillis() / 1000 + TOKEN_EXPIRATION_IN_SECONDS);

        // Generate token allowing caller and receiver to publish and subscribe to audio
        String token = tokenBuilder.buildTokenWithUid(
                appId,
                appCertificate,
                channelName,
                0,
                Role.Role_Publisher,
                timestamp
        );

        log.info("Generated Agora RTC token for channel: {} (userAccount: {})", channelName, userAccount);
        return token;
    }

    public String getAppId() {
        return appId;
    }

    private IllegalStateException excitingIllegalStateException(String message) {
        return new IllegalStateException(message);
    }
}
