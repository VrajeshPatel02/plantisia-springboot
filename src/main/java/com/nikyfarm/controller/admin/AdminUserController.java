package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.UserResponseDto;
import com.nikyfarm.dto.UserUpdateRequestDto;
import com.nikyfarm.entity.User;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin UI for User management. No "create user" form -- registration
 * (POST /api/auth/register) owns account creation; admins can only view/edit
 * role+active-status/delete existing accounts.
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.getAll());
        return "admin/users/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserResponseDto existing = userService.getById(id);
        UserUpdateRequestDto dto = new UserUpdateRequestDto();
        dto.setName(existing.name());
        dto.setEmail(existing.email());
        dto.setRole(existing.role());
        dto.setIsActive(existing.isActive());
        model.addAttribute("user", dto);
        addFormReferenceData(model, id);
        return "admin/users/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("user") UserUpdateRequestDto request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, id);
            return "admin/users/form";
        }
        try {
            userService.updateByAdmin(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");
            return "redirect:/admin/users";
        } catch (DuplicateResourceException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, id);
            return "admin/users/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void addFormReferenceData(Model model, Long userId) {
        model.addAttribute("userId", userId);
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("formAction", "/admin/users/" + userId);
    }
}
