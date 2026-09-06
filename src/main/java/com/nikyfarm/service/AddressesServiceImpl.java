package com.nikyfarm.service;

import com.nikyfarm.dto.AddressRequestDto;
import com.nikyfarm.dto.AddressResponseDto;
import com.nikyfarm.entity.Addresses;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.AddressMapper;
import com.nikyfarm.repository.AddressesRepository;
import com.nikyfarm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressesServiceImpl implements AddressesService {

    private final AddressesRepository addressesRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AddressResponseDto createAddress(AddressRequestDto requestDto) {
        User user = findUserOrThrow(requestDto.getUserId());

        if (Boolean.TRUE.equals(requestDto.getIsDefault())) {
            unsetExistingDefaults(user.getId());
        }

        Addresses address = new Addresses();
        address.setUser(user);
        applyDto(address, requestDto);

        return AddressMapper.toResponseDto(addressesRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponseDto updateAddress(Long addressId, AddressRequestDto requestDto, Long currentUserId, boolean isAdmin) {
        Addresses address = findAddressOrThrow(addressId);
        assertAccessPermission(address, currentUserId, isAdmin);

        if (Boolean.TRUE.equals(requestDto.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            unsetExistingDefaults(address.getUser().getId());
        }

        applyDto(address, requestDto);

        return AddressMapper.toResponseDto(addressesRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponseDto getAddressById(Long addressId, Long currentUserId, boolean isAdmin) {
        Addresses address = findAddressOrThrow(addressId);
        assertAccessPermission(address, currentUserId, isAdmin);
        return AddressMapper.toResponseDto(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponseDto> getAddressesByUserId(Long userId) {
        assertUserExists(userId);
        return addressesRepository.findByUserId(userId).stream()
                .map(AddressMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponseDto getDefaultAddressByUserId(Long userId) {
        assertUserExists(userId);
        Addresses defaultAddress = addressesRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No default address found for user id: " + userId));
        return AddressMapper.toResponseDto(defaultAddress);
    }

    @Override
    @Transactional
    public AddressResponseDto setDefaultAddress(Long addressId, Long currentUserId, boolean isAdmin) {
        Addresses address = findAddressOrThrow(addressId);
        assertAccessPermission(address, currentUserId, isAdmin);

        unsetExistingDefaults(address.getUser().getId());
        address.setIsDefault(true);

        return AddressMapper.toResponseDto(addressesRepository.save(address));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddressResponseDto> getAllAddresses(Pageable pageable) {
        return addressesRepository.findAll(pageable)
                .map(AddressMapper::toResponseDto);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId, Long currentUserId, boolean isAdmin) {
        Addresses address = findAddressOrThrow(addressId);
        assertAccessPermission(address, currentUserId, isAdmin);
        addressesRepository.delete(address);
    }

    private void applyDto(Addresses address, AddressRequestDto dto) {
        address.setFullName(dto.getFullName());
        address.setPhone(dto.getPhone());
        address.setLine1(dto.getLine1());
        address.setLine2(dto.getLine2());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPostalCode(dto.getPostalCode());
        address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : Boolean.FALSE);
    }

    private void unsetExistingDefaults(Long userId) {
        addressesRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(address -> {
                    address.setIsDefault(false);
                    addressesRepository.save(address);
                });
    }

    private void assertAccessPermission(Addresses address, Long currentUserId, boolean isAdmin) {
        if (!isAdmin && !address.getUser().getId().equals(currentUserId)) {
            // Throwing 404 on ownership failure to avoid exposing target resource presence
            throw new ResourceNotFoundException("Address not found with id: " + address.getId());
        }
    }

    private void assertUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private Addresses findAddressOrThrow(Long id) {
        return addressesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}