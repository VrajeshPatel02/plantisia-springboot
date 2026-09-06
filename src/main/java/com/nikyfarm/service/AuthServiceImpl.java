package com.nikyfarm.service;

import com.nikyfarm.dto.AuthResponseDto;
import com.nikyfarm.dto.LoginRequestDto;
import com.nikyfarm.dto.RegisterRequestDto;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.InvalidCredentialsException;
import com.nikyfarm.repository.UserRepository;
import com.nikyfarm.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("This account has been deactivated");
        }

        return buildAuthResponse(user);
    }

    private AuthResponseDto buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                Instant.now().plusMillis(jwtService.expirationMs()), ZoneId.systemDefault());
        return new AuthResponseDto(token, "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole(), expiresAt);
    }
}
