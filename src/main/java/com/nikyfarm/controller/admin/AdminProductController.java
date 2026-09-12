package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.ProductRequestDto;
import com.nikyfarm.dto.ProductResponseDto;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductMapper;
import com.nikyfarm.service.CategoryService;
import com.nikyfarm.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin UI for Product CRUD. Delegates to the same {@link ProductService} used by
 * the JSON REST API. Variants, images and EAV attribute values are managed on their own nested
 * screens (see {@code AdminProductVariantController}, {@code AdminProductImageController}, {@code
 * AdminProductAttributeValueController}).
 */
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private static final int PAGE_SIZE = 10;

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("products", productService.getProducts(null, pageable));
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        ProductRequestDto product = new ProductRequestDto();
        product.setIsActive(true);
        model.addAttribute("product", product);
        addFormReferenceData(model, "/admin/products", false);
        return "admin/products/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("product") ProductRequestDto product,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, "/admin/products", false);
            return "admin/products/form";
        }
        try {
            ProductResponseDto created = productService.createProduct(product);
            // Every product should have at least one variant (even a single "default" size/pack),
            // so route straight into adding one instead of back to the list.
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Product created successfully. Now add at least one variant (e.g. its size or pack).");
            return "redirect:/admin/products/" + created.id() + "/variants/new";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, "/admin/products", false);
            return "admin/products/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductResponseDto existing = productService.getProductById(id);
        model.addAttribute("product", ProductMapper.toRequestDto(existing));
        addFormReferenceData(model, "/admin/products/" + id, true);
        return "admin/products/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") ProductRequestDto product,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, "/admin/products/" + id, true);
            return "admin/products/form";
        }
        try {
            productService.updateProduct(id, product);
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully.");
            return "redirect:/admin/products";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, "/admin/products/" + id, true);
            return "admin/products/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    private void addFormReferenceData(Model model, String formAction, boolean isEdit) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEdit", isEdit);
    }
}
