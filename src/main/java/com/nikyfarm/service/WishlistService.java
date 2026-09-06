package com.nikyfarm.service;

import com.nikyfarm.dto.WishlistRequestDto;
import com.nikyfarm.dto.WishlistResponseDto;

import java.util.List;

public interface WishlistService {

    WishlistResponseDto add(Long userId, WishlistRequestDto dto);

    List<WishlistResponseDto> getMine(Long userId);

    void remove(Long userId, Long itemId);

    /** Admin: null userId returns every wishlist item across all users. */
    List<WishlistResponseDto> getAll(Long userId);

    /** Admin: removes any item regardless of owner. */
    void removeByAdmin(Long itemId);
}
