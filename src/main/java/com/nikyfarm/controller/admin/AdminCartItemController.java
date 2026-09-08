package com.nikyfarm.controller.admin;

import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin view of cart items across every user's cart — view + remove only, same shape as
 * AdminWishlistController (admins don't add items to someone else's cart). Unchanged
 * from before at the HTTP/template level; only the service behind it now reads through
 * the Cart aggregate instead of a flat CartItems table.
 */
@Controller
@RequestMapping("/admin/cart-items")
@RequiredArgsConstructor
public class AdminCartItemController {

    private final CartService cartService;

    @GetMapping
    public String list(@RequestParam(required = false) Long userId, Model model) {
        model.addAttribute("items", cartService.listAllItems(userId));
        model.addAttribute("selectedUserId", userId);
        return "admin/cart-items/list";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            cartService.deleteItemAsAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cart item removed.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/cart-items";
    }
}
