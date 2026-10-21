package com.nikyfarm.service;

import com.nikyfarm.dto.AuthResponseDto;
import com.nikyfarm.dto.GoogleLoginRequestDto;
import com.nikyfarm.dto.LoginRequestDto;
import com.nikyfarm.dto.RegisterRequestDto;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.InvalidCredentialsException;
import com.nikyfarm.repository.UserRepository;
import com.nikyfarm.security.GoogleTokenVerifier;
import com.nikyfarm.security.GoogleUserInfo;
import com.nikyfarm.security.JwtService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("An account with email '" + dto.getEmail() + "' already exists");
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.CUSTOMER);
        user.setIsActive(true);
        User saved = userRepository.save(user);

        return buildAuthResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto dto) {
        User user = userRepository
                .findByEmail(dto.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("This account has been deactivated");
        }

        return buildAuthResponse(user);
    }

    /**
     * Sign in (or sign up) with a Google ID token. Lookup order:
     *
     * <ol>
     *   <li>user already linked to this Google account (by {@code google_id}) -> log in
     *   <li>existing user with the same (Google-verified) email -> link the Google account, log in
     *   <li>otherwise -> create a new CUSTOMER account
     * </ol>
     */
    @Override
    @Transactional
    public AuthResponseDto googleLogin(GoogleLoginRequestDto dto) {
        GoogleUserInfo info = googleTokenVerifier.verify(dto.getIdToken());

        User user = userRepository.findByGoogleId(info.googleId()).orElseGet(() -> linkOrCreate(info));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("This account has been deactivated");
        }
        return buildAuthResponse(user);
    }

    private User linkOrCreate(GoogleUserInfo info) {
        Optional<User> existing = userRepository.findByEmail(info.email());

        if (existing.isPresent()) {
            User user = existing.get();
            // Same email but already tied to a different Google account: refuse rather than overwrite.
            if (user.getGoogleId() != null && !user.getGoogleId().equals(info.googleId())) {
                throw new InvalidCredentialsException("This email is linked to a different Google account");
            }
            // Registration does not verify email ownership, so a password set before this point may
            // belong to someone who never controlled this mailbox. Google has just proven ownership,
            // so discard the old password: from now on this account signs in with Google.
            user.setGoogleId(info.googleId());
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            return userRepository.save(user);
        }

        User user = new User();
        user.setName(info.name());
        user.setEmail(info.email());
        user.setGoogleId(info.googleId());
        // Google-only accounts have no password. A random, never-disclosed hash keeps password_hash
        // populated (in case the column is NOT NULL) while making password login impossible.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(User.Role.CUSTOMER);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    private AuthResponseDto buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        LocalDateTime expiresAt =
                LocalDateTime.ofInstant(Instant.now().plusMillis(jwtService.expirationMs()), ZoneId.systemDefault());
        return new AuthResponseDto(
                token, "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole(), expiresAt);
    }
}