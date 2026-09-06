package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.AttributeRequestDto;
import com.nikyfarm.dto.AttributeResponseDto;
import com.nikyfarm.entity.Attribute;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.AttributeMapper;
import com.nikyfarm.service.AttributeService;
import com.nikyfarm.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin UI for Attribute CRUD. Delegates to the same {@link AttributeService}
 * used by the JSON REST API.
 */
@Controller
@RequestMapping("/admin/attributes")
@RequiredArgsConstructor
public class AdminAttributeController {

    private final AttributeService attributeService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId, Model model) {
        model.addAttribute("attributes", attributeService.getAttributes(categoryId));
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        return "admin/attributes/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("attribute", new AttributeRequestDto());
        addFormReferenceData(model, "/admin/attributes", false);
        return "admin/attributes/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("attribute") AttributeRequestDto request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, "/admin/attributes", false);
            return "admin/attributes/form";
        }
        try {
            attributeService.createAttribute(request);
            redirectAttributes.addFlashAttribute("successMessage", "Attribute created successfully.");
            return "redirect:/admin/attributes";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, "/admin/attributes", false);
            return "admin/attributes/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AttributeResponseDto existing = attributeService.getAttributeById(id);
        model.addAttribute("attribute", AttributeMapper.toRequestDto(existing));
        addFormReferenceData(model, "/admin/attributes/" + id, true);
        return "admin/attributes/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("attribute") AttributeRequestDto request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, "/admin/attributes/" + id, true);
            return "admin/attributes/form";
        }
        try {
            attributeService.updateAttribute(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Attribute updated successfully.");
            return "redirect:/admin/attributes";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, "/admin/attributes/" + id, true);
            return "admin/attributes/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            attributeService.deleteAttribute(id);
            redirectAttributes.addFlashAttribute("successMessage", "Attribute deleted successfully.");
        } catch (BusinessRuleViolationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/attributes";
    }

    private void addFormReferenceData(Model model, String formAction, boolean isEdit) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("inputTypes", Attribute.InputType.values());
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEdit", isEdit);
    }
}
