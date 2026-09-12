package com.nikyfarm.service;

import com.nikyfarm.dto.UserResponseDto;
import com.nikyfarm.dto.UserSelfUpdateRequestDto;
import com.nikyfarm.dto.UserUpdateRequestDto;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.UserMapper;
import com.nikyfarm.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        return UserMapper.toResponseDto(findUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAll() {
        return userRepository.findAll().stream().map(UserMapper::toResponseDto).toList();
    }

    @Override
    @Transactional
    public UserResponseDto updateSelf(Long id, UserSelfUpdateRequestDto dto) {
        User user = findUserOrThrow(id);
        assertEmailAvailable(dto.getEmail(), id);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        return UserMapper.toResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponseDto updateByAdmin(Long id, UserUpdateRequestDto dto) {
        User user = findUserOrThrow(id);
        assertEmailAvailable(dto.getEmail(), id);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setIsActive(dto.getIsActive());
        return UserMapper.toResponseDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(findUserOrThrow(id));
    }

    private void assertEmailAvailable(String email, Long excludeId) {
        if (userRepository.existsByEmailAndIdNot(email, excludeId)) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists");
        }
    }

    private User findUserOrThrow(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
