package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.ProductVariantRequestDto;
import com.nikyfarm.dto.ProductVariantResponseDto;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductVariantMapper;
import com.nikyfarm.service.ProductService;
import com.nikyfarm.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin UI for ProductVariant CRUD, nested under a product. Delegates to the same
 * {@link ProductVariantService} used by the JSON REST API.
 */
@Controller
@RequestMapping("/admin/products/{productId}/variants")
@RequiredArgsConstructor
public class AdminProductVariantController {

    private final ProductVariantService productVariantService;
    private final ProductService productService;

    @GetMapping
    public String list(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("variants", productVariantService.getVariantsByProduct(productId));
        return "admin/variants/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long productId, Model model) {
        ProductVariantRequestDto variant = new ProductVariantRequestDto();
        variant.setIsActive(true);
        variant.setIsDefault(false);
        model.addAttribute("variant", variant);
        addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants", false);
        return "admin/variants/form";
    }

    @PostMapping
    public String create(
            @PathVariable Long productId,
            @Valid @ModelAttribute("variant") ProductVariantRequestDto variant,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants", false);
            return "admin/variants/form";
        }
        try {
            productVariantService.createVariant(productId, variant);
            redirectAttributes.addFlashAttribute("successMessage", "Variant created successfully.");
            return "redirect:/admin/products/" + productId + "/variants";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants", false);
            return "admin/variants/form";
        }
    }

    @GetMapping("/{variantId}/edit")
    public String editForm(@PathVariable Long productId, @PathVariable Long variantId, Model model) {
        ProductVariantResponseDto existing = productVariantService.getVariantById(productId, variantId);
        model.addAttribute("variant", ProductVariantMapper.toRequestDto(existing));
        addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants/" + variantId, true);
        return "admin/variants/form";
    }

    @PostMapping("/{variantId}")
    public String update(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @ModelAttribute("variant") ProductVariantRequestDto variant,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants/" + variantId, true);
            return "admin/variants/form";
        }
        try {
            productVariantService.updateVariant(productId, variantId, variant);
            redirectAttributes.addFlashAttribute("successMessage", "Variant updated successfully.");
            return "redirect:/admin/products/" + productId + "/variants";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants/" + variantId, true);
            return "admin/variants/form";
        }
    }

    @PostMapping("/{variantId}/delete")
    public String delete(
            @PathVariable Long productId, @PathVariable Long variantId, RedirectAttributes redirectAttributes) {
        try {
            productVariantService.deleteVariant(productId, variantId);
            redirectAttributes.addFlashAttribute("successMessage", "Variant deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/variants";
    }

    private void addFormReferenceData(Model model, Long productId, String formAction, boolean isEdit) {
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEdit", isEdit);
    }
}
