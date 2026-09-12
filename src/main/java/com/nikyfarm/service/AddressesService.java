package com.nikyfarm.service;

import com.nikyfarm.dto.AddressRequestDto;
import com.nikyfarm.dto.AddressResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddressesService {

    AddressResponseDto createAddress(AddressRequestDto requestDto);

    AddressResponseDto updateAddress(Long addressId, AddressRequestDto requestDto, Long currentUserId, boolean isAdmin);

    AddressResponseDto getAddressById(Long addressId, Long currentUserId, boolean isAdmin);

    List<AddressResponseDto> getAddressesByUserId(Long userId);

    AddressResponseDto getDefaultAddressByUserId(Long userId);

    Page<AddressResponseDto> getAllAddresses(Pageable pageable);

    AddressResponseDto setDefaultAddress(Long addressId, Long currentUserId, boolean isAdmin);

    void deleteAddress(Long addressId, Long currentUserId, boolean isAdmin);
}
