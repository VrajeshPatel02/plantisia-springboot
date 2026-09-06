package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.AttributeOptionRequestDto;
import com.nikyfarm.dto.AttributeOptionResponseDto;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.AttributeOptionMapper;
import com.nikyfarm.service.AttributeOptionService;
import com.nikyfarm.service.AttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin UI for AttributeOptions CRUD, nested under an attribute.
 * Delegates to the same {@link AttributeOptionService} used by the JSON REST API.
 */
@Controller
@RequestMapping("/admin/attributes/{attributeId}/options")
@RequiredArgsConstructor
public class AdminAttributeOptionController {

    private final AttributeOptionService attributeOptionService;
    private final AttributeService attributeService;

    @GetMapping
    public String list(@PathVariable Long attributeId, Model model) {
        model.addAttribute("attribute", attributeService.getAttributeById(attributeId));
        model.addAttribute("options", attributeOptionService.getOptionsByAttribute(attributeId));
        return "admin/attribute-options/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long attributeId, Model model) {
        model.addAttribute("option", new AttributeOptionRequestDto());
        addFormReferenceData(model, attributeId, "/admin/attributes/" + attributeId + "/options", false);
        return "admin/attribute-options/form";
    }

    @PostMapping
    public String create(@PathVariable Long attributeId,
                          @Valid @ModelAttribute("option") AttributeOptionRequestDto request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, attributeId, "/admin/attributes/" + attributeId + "/options", false);
            return "admin/attribute-options/form";
        }
        try {
            attributeOptionService.createOption(attributeId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Option created successfully.");
            return "redirect:/admin/attributes/" + attributeId + "/options";
        } catch (DuplicateResourceException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, attributeId, "/admin/attributes/" + attributeId + "/options", false);
            return "admin/attribute-options/form";
        }
    }

    @GetMapping("/{optionId}/edit")
    public String editForm(@PathVariable Long attributeId, @PathVariable Long optionId, Model model) {
        AttributeOptionResponseDto existing = attributeOptionService.getOptionById(attributeId, optionId);
        model.addAttribute("option", AttributeOptionMapper.toRequestDto(existing));
        addFormReferenceData(model, attributeId, "/admin/attributes/" + attributeId + "/options/" + optionId, true);
        return "admin/attribute-options/form";
    }

    @PostMapping("/{optionId}")
    public String update(@PathVariable Long attributeId,
                          @PathVariable Long optionId,
                          @Valid @ModelAttribute("option") AttributeOptionRequestDto request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, attributeId, "/admin/attributes/" + attributeId + "/options/" + optionId, true);
            return "admin/attribute-options/form";
        }
        try {
            attributeOptionService.updateOption(attributeId, optionId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Option updated successfully.");
            return "redirect:/admin/attributes/" + attributeId + "/options";
        } catch (DuplicateResourceException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, attributeId, "/admin/attributes/" + attributeId + "/options/" + optionId, true);
            return "admin/attribute-options/form";
        }
    }

    @PostMapping("/{optionId}/delete")
    public String delete(@PathVariable Long attributeId, @PathVariable Long optionId, RedirectAttributes redirectAttributes) {
        try {
            attributeOptionService.deleteOption(attributeId, optionId);
            redirectAttributes.addFlashAttribute("successMessage", "Option deleted successfully.");
        } catch (BusinessRuleViolationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/attributes/" + attributeId + "/options";
    }

    private void addFormReferenceData(Model model, Long attributeId, String formAction, boolean isEdit) {
        model.addAttribute("attribute", attributeService.getAttributeById(attributeId));
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEdit", isEdit);
    }
}
