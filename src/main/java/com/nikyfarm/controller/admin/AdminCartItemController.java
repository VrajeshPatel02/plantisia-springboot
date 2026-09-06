package com.nikyfarm.controller.admin;

import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.CartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** View + remove only -- admins don't manually add items to someone else's cart. */
@Controller
@RequestMapping("/admin/cart-items")
@RequiredArgsConstructor
public class AdminCartItemController {

    private final CartItemService cartItemService;

    @GetMapping
    public String list(@RequestParam(required = false) Long userId, Model model) {
        model.addAttribute("items", cartItemService.getAll(userId));
        model.addAttribute("selectedUserId", userId);
        return "admin/cart-items/list";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            cartItemService.removeByAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cart item removed successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/cart-items";
    }
}
