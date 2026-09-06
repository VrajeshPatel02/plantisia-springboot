package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.CategoryRequestDto;
import com.nikyfarm.dto.CategoryResponseDto;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ImageUploadException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.CategoryMapper;
import com.nikyfarm.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin UI for Category CRUD. Delegates to the same {@link CategoryService}
 * used by the JSON REST API; business-rule/not-found/duplicate exceptions are caught here
 * (rather than left to GlobalExceptionHandler) so the user gets a re-rendered form/list with
 * a friendly message instead of a raw JSON error body.
 */
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        CategoryRequestDto category = new CategoryRequestDto();
        category.setIsActive(true);
        category.setSortOrder(0);
        model.addAttribute("category", category);
        addFormReferenceData(model, null, "/admin/categories", false);
        return "admin/categories/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("category") CategoryRequestDto category,
                          BindingResult bindingResult,
                          @RequestParam(value = "file", required = false) MultipartFile file,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, null, "/admin/categories", false);
            return "admin/categories/form";
        }
        try {
            categoryService.createCategory(category, file);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully.");
            return "redirect:/admin/categories";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException | ImageUploadException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, null, "/admin/categories", false);
            return "admin/categories/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CategoryResponseDto existing = categoryService.getCategoryById(id);
        model.addAttribute("category", CategoryMapper.toRequestDto(existing));
        model.addAttribute("currentImageUrl", existing.imageUrl());
        addFormReferenceData(model, id, "/admin/categories/" + id, true);
        return "admin/categories/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("category") CategoryRequestDto category,
                          BindingResult bindingResult,
                          @RequestParam(value = "file", required = false) MultipartFile file,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("currentImageUrl", categoryService.getCategoryById(id).imageUrl());
            addFormReferenceData(model, id, "/admin/categories/" + id, true);
            return "admin/categories/form";
        }
        try {
            categoryService.updateCategory(id, category, file);
            redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully.");
            return "redirect:/admin/categories";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException | ImageUploadException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("currentImageUrl", categoryService.getCategoryById(id).imageUrl());
            addFormReferenceData(model, id, "/admin/categories/" + id, true);
            return "admin/categories/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully.");
        } catch (BusinessRuleViolationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    private void addFormReferenceData(Model model, Long excludeId, String formAction, boolean isEdit) {
        var parents = categoryService.getAllCategories().stream()
                .filter(c -> excludeId == null || !c.id().equals(excludeId))
                .toList();
        model.addAttribute("parentCategories", parents);
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEdit", isEdit);
    }
}
