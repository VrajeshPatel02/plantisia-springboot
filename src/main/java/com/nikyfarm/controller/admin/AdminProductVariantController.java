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

/** Admin variant CRUD; keeps the originating product-list page/search across navigation. */
@Controller
@RequestMapping("/admin/products/{productId}/variants")
@RequiredArgsConstructor
public class AdminProductVariantController {

    private final ProductVariantService productVariantService;
    private final ProductService productService;

    @GetMapping
    public String list(@PathVariable Long productId,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String search,
                       Model model) {
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("variants", productVariantService.getVariantsByProduct(productId));
        addReturnState(model, productId, page, search);
        return "admin/variants/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long productId,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) String search,
                          Model model) {
        ProductVariantRequestDto variant = new ProductVariantRequestDto();
        variant.setIsActive(true);
        variant.setIsDefault(false);
        model.addAttribute("variant", variant);
        addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants", false, page, search);
        return "admin/variants/form";
    }

    @PostMapping
    public String create(@PathVariable Long productId,
                         @Valid @ModelAttribute("variant") ProductVariantRequestDto variant,
                         BindingResult bindingResult,
                         @RequestParam(defaultValue = "0") int returnPage,
                         @RequestParam(required = false) String search,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants", false, returnPage, search);
            return "admin/variants/form";
        }
        try {
            productVariantService.createVariant(productId, variant);
            redirectAttributes.addFlashAttribute("successMessage", "Variant created successfully.");
            return "redirect:" + buildEditUrl(productId, returnPage, search);
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants", false, returnPage, search);
            return "admin/variants/form";
        }
    }

    @GetMapping("/{variantId}/edit")
    public String editForm(@PathVariable Long productId,
                           @PathVariable Long variantId,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) String search,
                           Model model) {
        ProductVariantResponseDto existing = productVariantService.getVariantById(productId, variantId);
        model.addAttribute("variant", ProductVariantMapper.toRequestDto(existing));
        addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants/" + variantId, true, page, search);
        return "admin/variants/form";
    }

    @PostMapping("/{variantId}")
    public String update(@PathVariable Long productId,
                         @PathVariable Long variantId,
                         @Valid @ModelAttribute("variant") ProductVariantRequestDto variant,
                         BindingResult bindingResult,
                         @RequestParam(defaultValue = "0") int returnPage,
                         @RequestParam(required = false) String search,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/variants/" + variantId, true, returnPage, search);
            model.addAttribute("variant", variant);
            return "admin/variants/form";
        }
        try {
            productVariantService.updateVariant(productId, variantId, variant);
            redirectAttributes.addFlashAttribute("successMessage", "Variant updated successfully.");
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildEditUrl(productId, returnPage, search);
    }

    @PostMapping("/{variantId}/delete")
    public String delete(@PathVariable Long productId,
                         @PathVariable Long variantId,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false) String search,
                         RedirectAttributes redirectAttributes) {
        try {
            productVariantService.deleteVariant(productId, variantId);
            redirectAttributes.addFlashAttribute("successMessage", "Variant deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildEditUrl(productId, page, search);
    }

    private void addFormReferenceData(Model model, Long productId, String formAction, boolean isEdit,
                                      int page, String search) {
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEdit", isEdit);
        addReturnState(model, productId, page, search);
    }

    private void addReturnState(Model model, Long productId, int page, String search) {
        int safePage = Math.max(page, 0);
        String query = search == null ? "" : search.trim();
        String editUrl = "/admin/products/" + productId + "/edit?page=" + safePage;
        String listUrl = "/admin/products?page=" + safePage;
        if (!query.isBlank()) {
            String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            editUrl += "&search=" + encoded;
            listUrl += "&search=" + encoded;
        }
        model.addAttribute("returnPage", safePage);
        model.addAttribute("search", query);
        model.addAttribute("productEditUrl", editUrl);
        model.addAttribute("listReturnUrl", listUrl);
    }

    private String buildEditUrl(Long productId, int page, String search) {
        String url = "/admin/products/" + productId + "/edit?page=" + Math.max(page, 0);
        String query = search == null ? "" : search.trim();
        if (!query.isBlank()) {
            url += "&search=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        }
        return url;
    }
}
