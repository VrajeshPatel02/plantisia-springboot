package com.nikyfarm.service;

import com.nikyfarm.dto.AuthResponseDto;
import com.nikyfarm.dto.LoginRequestDto;
import com.nikyfarm.dto.RegisterRequestDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto dto);

    AuthResponseDto login(LoginRequestDto dto);
}
