package com.nikyfarm.controller.admin;

import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * View + remove only -- admins don't manually add items to someone else's wishlist,
 * so there's no create form here (see plan: "real-world scope").
 */
@Controller
@RequestMapping("/admin/wishlist")
@RequiredArgsConstructor
public class AdminWishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public String list(@RequestParam(required = false) Long userId, Model model) {
        model.addAttribute("items", wishlistService.getAll(userId));
        model.addAttribute("selectedUserId", userId);
        return "admin/wishlist/list";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            wishlistService.removeByAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "Wishlist item removed successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/wishlist";
    }
}
