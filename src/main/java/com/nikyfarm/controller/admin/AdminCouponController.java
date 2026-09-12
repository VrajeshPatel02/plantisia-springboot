package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.CouponRequestDto;
import com.nikyfarm.dto.CouponResponseDto;
import com.nikyfarm.entity.Coupon.DiscountType;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.CouponService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    public String listCoupons(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        Page<CouponResponseDto> couponPage = couponService.getAllCoupons(PageRequest.of(page, size));
        model.addAttribute("coupons", couponPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", couponPage.getTotalPages());
        return "admin/coupons/list";
    }

    @GetMapping("/{id}")
    public String viewCouponDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            CouponResponseDto coupon = couponService.getCouponById(id);
            model.addAttribute("coupon", coupon);
            return "admin/coupons/detail";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/coupons";
        }
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("couponForm", new CouponRequestDto());
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("isEdit", false);
        return "admin/coupons/form";
    }

    @PostMapping("/create")
    public String createCoupon(
            @Valid @ModelAttribute("couponForm") CouponRequestDto requestDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isEdit", false);
            return "admin/coupons/form";
        }

        try {
            couponService.createCoupon(requestDto);
            redirectAttributes.addFlashAttribute("successMessage", "Coupon created successfully!");
            return "redirect:/admin/coupons";
        } catch (BusinessRuleViolationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isEdit", false);
            return "admin/coupons/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            CouponResponseDto coupon = couponService.getCouponById(id);
            CouponRequestDto formDto = new CouponRequestDto();
            formDto.setCode(coupon.getCode());
            formDto.setDescription(coupon.getDescription());
            formDto.setDiscountType(coupon.getDiscountType());
            formDto.setDiscountValue(coupon.getDiscountValue());
            formDto.setMinOrderValue(coupon.getMinOrderValue());
            formDto.setMaxUses(coupon.getMaxUses());
            formDto.setValidFrom(coupon.getValidFrom());
            formDto.setValidUntil(coupon.getValidUntil());
            formDto.setIsActive(coupon.getIsActive());

            model.addAttribute("couponForm", formDto);
            model.addAttribute("couponId", id);
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isEdit", true);
            return "admin/coupons/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/coupons";
        }
    }

    @PostMapping("/{id}/edit")
    public String updateCoupon(
            @PathVariable Long id,
            @Valid @ModelAttribute("couponForm") CouponRequestDto requestDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("couponId", id);
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isEdit", true);
            return "admin/coupons/form";
        }

        try {
            couponService.updateCoupon(id, requestDto);
            redirectAttributes.addFlashAttribute("successMessage", "Coupon updated successfully!");
            return "redirect:/admin/coupons";
        } catch (BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("couponId", id);
            model.addAttribute("discountTypes", DiscountType.values());
            model.addAttribute("isEdit", true);
            return "admin/coupons/form";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggleCouponStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            couponService.toggleCouponStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Coupon status updated");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/delete")
    public String deleteCoupon(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            couponService.deleteCoupon(id);
            redirectAttributes.addFlashAttribute("successMessage", "Coupon deleted successfully");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @GetMapping("/check-code")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkCodeAvailability(@RequestParam String code) {
        try {
            couponService.getCouponByCode(code);
            return ResponseEntity.ok(Map.of("exists", true));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(Map.of("exists", false));
        }
    }
}
