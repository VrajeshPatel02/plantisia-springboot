package com.nikyfarm.service;

import com.nikyfarm.dto.WishlistRequestDto;
import com.nikyfarm.dto.WishlistResponseDto;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.User;
import com.nikyfarm.entity.Wishlist;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.WishlistMapper;
import com.nikyfarm.repository.ProductRepository;
import com.nikyfarm.repository.UserRepository;
import com.nikyfarm.repository.WishlistRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public WishlistResponseDto add(Long userId, WishlistRequestDto dto) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, dto.getProductId())) {
            throw new DuplicateResourceException("This product is already in your wishlist");
        }
        Product product = productRepository
                .findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + dto.getProductId()));
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        return WishlistMapper.toResponseDto(wishlistRepository.save(wishlist));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponseDto> getMine(Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(WishlistMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void remove(Long userId, Long itemId) {
        Wishlist wishlist = findOwnedOrThrow(userId, itemId);
        wishlistRepository.delete(wishlist);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponseDto> getAll(Long userId) {
        List<Wishlist> items = userId != null ? wishlistRepository.findByUserId(userId) : wishlistRepository.findAll();
        return items.stream().map(WishlistMapper::toResponseDto).toList();
    }

    @Override
    @Transactional
    public void removeByAdmin(Long itemId) {
        Wishlist wishlist = wishlistRepository
                .findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + itemId));
        wishlistRepository.delete(wishlist);
    }

    private Wishlist findOwnedOrThrow(Long userId, Long itemId) {
        Wishlist wishlist = wishlistRepository
                .findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found with id: " + itemId));
        if (wishlist.getUser() == null || !wishlist.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Wishlist item not found with id: " + itemId);
        }
        return wishlist;
    }
}
