package com.nikyfarm.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nikyfarm.exception.InvalidCredentialsException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Verifies Google ID tokens sent by the Flutter clients (Android + web). Checks the signature
 * (against Google's public keys), issuer, expiry and that the token's audience is one of our
 * configured OAuth client IDs.
 */
@Slf4j
@Component
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${google.oauth.client-id}") String clientIds) {
        List<String> audience = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .toList();
        if (audience.isEmpty()) {
            throw new IllegalStateException("google.oauth.client-id must contain at least one Google client ID");
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(audience)
                .build();
    }

    /** @throws InvalidCredentialsException if the token is malformed, expired, forged or for another app. */
    public GoogleUserInfo verify(String idTokenString) {
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            log.warn("Google ID token verification failed: {}", e.getMessage());
            throw new InvalidCredentialsException("Invalid Google ID token");
        }
        if (idToken == null) {
            throw new InvalidCredentialsException("Invalid Google ID token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        if (email == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidCredentialsException("Google account email is missing or not verified");
        }

        String name = (String) payload.get("name");
        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf('@'));
        }
        String picture = (String) payload.get("picture");

        return new GoogleUserInfo(payload.getSubject(), email, name, picture);
    }
}