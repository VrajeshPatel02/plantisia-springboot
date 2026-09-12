package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.AddressResponseDto;
import com.nikyfarm.service.AddressesService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Read-only view -- admins can inspect customer addresses for order verification and customer
 * support, but cannot modify address records directly.
 */
@Controller
@RequestMapping("/admin/addresses")
@RequiredArgsConstructor
public class AdminAddressesController {

    private final AddressesService addressesService;

    @GetMapping
    public String list(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        if (userId != null) {
            // Unpaginated user-specific filter
            List<AddressResponseDto> addresses = addressesService.getAddressesByUserId(userId);
            model.addAttribute("addresses", addresses);
            model.addAttribute("isPaginated", false);
        } else {
            // Paginated list of all addresses in system
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<AddressResponseDto> addressPage = addressesService.getAllAddresses(pageable);

            model.addAttribute("addresses", addressPage.getContent());
            model.addAttribute("currentPage", addressPage.getNumber());
            model.addAttribute("totalPages", addressPage.getTotalPages());
            model.addAttribute("totalItems", addressPage.getTotalElements());
            model.addAttribute("isPaginated", true);
        }

        model.addAttribute("selectedUserId", userId);
        return "admin/addresses/list";
    }
}
