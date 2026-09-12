package com.nikyfarm.controller;

import com.nikyfarm.dto.AddressRequestDto;
import com.nikyfarm.dto.AddressResponseDto;
import com.nikyfarm.service.AddressesService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressesService addressesService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<AddressResponseDto> createAddress(
            @Valid @RequestBody AddressRequestDto requestDto,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {

        // Ensure non-admin users can only create addresses for themselves
        if (!isAdmin(authentication)) {
            requestDto.setUserId(currentUserId);
        }

        AddressResponseDto created = addressesService.createAddress(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<AddressResponseDto> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequestDto requestDto,
            @AuthenticationPrincipal Long currentUserId,
            Authentication authentication) {

        AddressResponseDto updated =
                addressesService.updateAddress(id, requestDto, currentUserId, isAdmin(authentication));
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<AddressResponseDto> getAddressById(
            @PathVariable Long id, @AuthenticationPrincipal Long currentUserId, Authentication authentication) {

        AddressResponseDto address = addressesService.getAddressById(id, currentUserId, isAdmin(authentication));
        return ResponseEntity.ok(address);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<AddressResponseDto>> getMyAddresses(@AuthenticationPrincipal Long currentUserId) {

        List<AddressResponseDto> addresses = addressesService.getAddressesByUserId(currentUserId);
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/me/default")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AddressResponseDto> getMyDefaultAddress(@AuthenticationPrincipal Long currentUserId) {

        AddressResponseDto address = addressesService.getDefaultAddressByUserId(currentUserId);
        return ResponseEntity.ok(address);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AddressResponseDto>> getAddressesByUserId(@PathVariable Long userId) {
        List<AddressResponseDto> addresses = addressesService.getAddressesByUserId(userId);
        return ResponseEntity.ok(addresses);
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<AddressResponseDto> setDefaultAddress(
            @PathVariable Long id, @AuthenticationPrincipal Long currentUserId, Authentication authentication) {

        AddressResponseDto address = addressesService.setDefaultAddress(id, currentUserId, isAdmin(authentication));
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id, @AuthenticationPrincipal Long currentUserId, Authentication authentication) {

        addressesService.deleteAddress(id, currentUserId, isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
