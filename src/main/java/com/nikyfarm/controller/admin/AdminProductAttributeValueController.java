package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.AttributeResponseDto;
import com.nikyfarm.dto.ProductAttributeValueRequestDto;
import com.nikyfarm.dto.ProductAttributeValueResponseDto;
import com.nikyfarm.dto.ProductResponseDto;
import com.nikyfarm.entity.Attribute;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductAttributeValueMapper;
import com.nikyfarm.service.AttributeOptionService;
import com.nikyfarm.service.AttributeService;
import com.nikyfarm.service.ProductAttributeValueService;
import com.nikyfarm.service.ProductService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin UI for assigning EAV attribute values to a product, nested under a product.
 * The create/edit form is dynamic: the visible input (free text vs. an option picker) depends on
 * the chosen attribute's {@link Attribute.InputType}, driven by a small inline script fed the
 * per-attribute metadata built here.
 */
@Controller
@RequestMapping("/admin/products/{productId}/attribute-values")
@RequiredArgsConstructor
public class AdminProductAttributeValueController {

    private final ProductAttributeValueService productAttributeValueService;
    private final ProductService productService;
    private final AttributeService attributeService;
    private final AttributeOptionService attributeOptionService;

    @GetMapping
    public String list(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("values", productAttributeValueService.getValuesByProduct(productId));
        return "admin/attribute-values/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long productId, @RequestParam(defaultValue = "0") int page, @RequestParam(required = false) String search, Model model) {
        model.addAttribute("value", new ProductAttributeValueRequestDto());
        addFormReferenceData(model, productId, "/admin/products/" + productId + "/attribute-values", false, page, search);
        return "admin/attribute-values/form";
    }

    @PostMapping
    public String create(
            @PathVariable Long productId,
            @Valid @ModelAttribute("value") ProductAttributeValueRequestDto request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @RequestParam(defaultValue = "0") int returnPage,
            @RequestParam(required = false) String search) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/attribute-values", false, returnPage, search);
            return "admin/attribute-values/form";
        }
        try {
            productAttributeValueService.createValue(productId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Attribute value added successfully.");
            return redirectToProductEdit(productId, returnPage, search);
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, productId, "/admin/products/" + productId + "/attribute-values", false, returnPage, search);
            return "admin/attribute-values/form";
        }
    }

    @GetMapping("/{valueId}/edit")
    public String editForm(@PathVariable Long productId, @PathVariable Long valueId, @RequestParam(defaultValue = "0") int page, @RequestParam(required = false) String search, Model model) {
        ProductAttributeValueResponseDto existing = productAttributeValueService.getValueById(productId, valueId);
        model.addAttribute("value", ProductAttributeValueMapper.toRequestDto(existing));
        addFormReferenceData(model, productId, "/admin/products/" + productId + "/attribute-values/" + valueId, true, page, search);
        return "admin/attribute-values/form";
    }

    @PostMapping("/{valueId}")
    public String update(
            @PathVariable Long productId,
            @PathVariable Long valueId,
            @Valid @ModelAttribute("value") ProductAttributeValueRequestDto request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            @RequestParam(defaultValue = "0") int returnPage,
            @RequestParam(required = false) String search) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(
                    model, productId, "/admin/products/" + productId + "/attribute-values/" + valueId, true, returnPage, search);
            return "admin/attribute-values/form";
        }
        try {
            productAttributeValueService.updateValue(productId, valueId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Attribute value updated successfully.");
            return redirectToProductEdit(productId, returnPage, search);
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(
                    model, productId, "/admin/products/" + productId + "/attribute-values/" + valueId, true, returnPage, search);
            return "admin/attribute-values/form";
        }
    }

    @PostMapping("/{valueId}/delete")
    public String delete(
            @PathVariable Long productId, @PathVariable Long valueId, RedirectAttributes redirectAttributes) {
        try {
            productAttributeValueService.deleteValue(productId, valueId);
            redirectAttributes.addFlashAttribute("successMessage", "Attribute value removed successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/attribute-values";
    }

    private void addFormReferenceData(Model model, Long productId, String formAction, boolean isEdit, int page, String search) {
        ProductResponseDto product = productService.getProductById(productId);
        List<AttributeResponseDto> attributes = attributeService.getAttributes(product.categoryId());

        Map<String, Object> attributeMeta = new LinkedHashMap<>();
        for (AttributeResponseDto attribute : attributes) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("inputType", attribute.inputType().name());
            if (attribute.inputType() == Attribute.InputType.SELECT) {
                List<Map<String, Object>> options =
                        attributeOptionService.getOptionsByAttribute(attribute.id()).stream()
                                .map(o -> {
                                    Map<String, Object> m = new LinkedHashMap<>();
                                    m.put("id", o.id());
                                    m.put("value", o.value());
                                    return m;
                                })
                                .toList();
                meta.put("options", options);
            }
            attributeMeta.put(String.valueOf(attribute.id()), meta);
        }

        model.addAttribute("product", product);
        model.addAttribute("attributes", attributes);
        model.addAttribute("attributeMetaJson", attributeMeta);
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("returnPage", Math.max(page, 0));
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("productEditUrl", buildProductEditUrl(productId, page, search));
    }

    private String redirectToProductEdit(Long productId, int page, String search) {
        return "redirect:" + buildProductEditUrl(productId, page, search);
    }

    private String buildProductEditUrl(Long productId, int page, String search) {
        String url = "/admin/products/" + productId + "/edit?page=" + Math.max(page, 0);
        String query = search == null ? "" : search.trim();
        return query.isBlank() ? url : url + "&search=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
    }
}
