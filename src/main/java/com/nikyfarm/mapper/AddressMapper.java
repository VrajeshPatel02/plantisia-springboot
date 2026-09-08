package com.nikyfarm.mapper;

import com.nikyfarm.dto.AddressRequestDto;
import com.nikyfarm.dto.AddressResponseDto;
import com.nikyfarm.entity.Addresses;

public final class  AddressMapper {

    private AddressMapper(){
    }

    public static AddressResponseDto toResponseDto(Addresses address) {
        return new AddressResponseDto(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getIsDefault(),
                address.getCreatedAt(),
                address.getUser() != null ? address.getUser().getId() : null
        );
    }

    /** Pre-fills an edit form with the current values of an existing address. */
    public static AddressRequestDto toRequestDto(AddressResponseDto dto) {
        AddressRequestDto request = new AddressRequestDto();
        request.setFullName(dto.fullName());
        request.setPhone(dto.phone());
        request.setLine1(dto.line1());
        request.setLine2(dto.line2());
        request.setCity(dto.city());
        request.setState(dto.state());
        request.setPostalCode(dto.postalCode());
        request.setIsDefault(dto.isDefault());
        return request;

    }
}
