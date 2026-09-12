package com.nikyfarm.service;

import com.nikyfarm.dto.UserResponseDto;
import com.nikyfarm.dto.UserSelfUpdateRequestDto;
import com.nikyfarm.dto.UserUpdateRequestDto;
import java.util.List;

public interface UserService {

    UserResponseDto getById(Long id);

    List<UserResponseDto> getAll();

    UserResponseDto updateSelf(Long id, UserSelfUpdateRequestDto dto);

    UserResponseDto updateByAdmin(Long id, UserUpdateRequestDto dto);

    void deleteUser(Long id);
}
