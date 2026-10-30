package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.ProductRequestDto;
import com.nikyfarm.dto.ProductResponseDto;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.mapper.ProductMapper;
import com.nikyfarm.service.CategoryService;
import com.nikyfarm.service.ProductService;
import com.nikyfarm.service.ProductImageService;
import com.nikyfarm.service.ProductVariantService;
import com.nikyfarm.service.ProductAttributeValueService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private static final int PAGE_SIZE = 10;

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductImageService productImageService;
    private final ProductVariantService productVariantService;
    private final ProductAttributeValueService productAttributeValueService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Model model) {
        int safePage = Math.max(page, 0);
        String query = search == null ? "" : search.trim();
        Pageable pageable = PageRequest.of(
                safePage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));

        model.addAttribute("products", productService.getProducts(
                null, query.isBlank() ? null : query, pageable));
        model.addAttribute("search", query);
        return "admin/products/list";
    }


    @PostMapping("/export")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam("fields") List<String> fields,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(defaultValue = "nikyfarm-products") String filename) {
        byte[] file = productService.exportProducts(fields, format);
        String safeName = (filename == null ? "" : filename).trim()
                .replaceAll("[^a-zA-Z0-9._-]+", "-").replaceAll("^-+|-+$", "");
        if (safeName.isBlank()) safeName = "nikyfarm-products";
        String extension = "csv".equalsIgnoreCase(format) ? "csv" : "xlsx";
        MediaType type = "csv".equalsIgnoreCase(format) ?
                new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8) :
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "." + extension + "\"")
                .contentType(type).contentLength(file.length).body(file);
    }

    @GetMapping("/new")
    public String newForm(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Model model) {
        ProductRequestDto product = new ProductRequestDto();
        product.setIsActive(true);
        model.addAttribute("product", product);
        addFormReferenceData(model, "/admin/products", false, page, search);
        return "admin/products/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("product") ProductRequestDto product,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "0") int returnPage,
            @RequestParam(required = false) String search,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, "/admin/products", false, returnPage, search);
            return "admin/products/form";
        }
        try {
            ProductResponseDto created = productService.createProduct(product);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product created. Add its images, variants and attributes from the product management page.");
            return "redirect:" + buildEditUrl(created.id(), returnPage, search);
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addFormReferenceData(model, "/admin/products", false, returnPage, search);
            return "admin/products/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            Model model) {
        ProductResponseDto existing = productService.getProductById(id);
        model.addAttribute("product", ProductMapper.toRequestDto(existing));
        model.addAttribute("productDetails", existing);
        addFormReferenceData(model, "/admin/products/" + id, true, page, search);
        model.addAttribute("returnPage", Math.max(page, 0));
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("listReturnUrl", buildListUrl(page, search));
        model.addAttribute("imagesUrl", "/admin/products/" + id + "/images");
        model.addAttribute("variantsUrl", "/admin/products/" + id + "/variants");
        model.addAttribute("attributesUrl", "/admin/products/" + id + "/attribute-values");

        // Load existing catalog records so the edit page can display section-wise lists
        // underneath the product details form.
        model.addAttribute("productImages", productImageService.getGeneralImages(id));
        List<com.nikyfarm.dto.ProductVariantResponseDto> variants =
                productVariantService.getVariantsByProduct(id);
        model.addAttribute("variants", variants);
        model.addAttribute("attributeValues", productAttributeValueService.getValuesByProduct(id));

        List<Map<String, Object>> variantImageGroups = variants.stream().map(variant -> {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("variant", variant);
            group.put("images", productImageService.getImagesByVariant(id, variant.id()));
            return group;
        }).toList();
        model.addAttribute("variantImageGroups", variantImageGroups);
        return "admin/products/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") ProductRequestDto product,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "0") int returnPage,
            @RequestParam(required = false) String search,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            ProductResponseDto existing = productService.getProductById(id);
            model.addAttribute("productDetails", existing);
            addFormReferenceData(model, "/admin/products/" + id, true, returnPage, search);
            model.addAttribute("returnPage", Math.max(returnPage, 0));
            model.addAttribute("search", search == null ? "" : search.trim());
            model.addAttribute("listReturnUrl", buildListUrl(returnPage, search));
            model.addAttribute("imagesUrl", "/admin/products/" + id + "/images");
            model.addAttribute("variantsUrl", "/admin/products/" + id + "/variants");
            model.addAttribute("attributesUrl", "/admin/products/" + id + "/attribute-values");
            model.addAttribute("productImages", productImageService.getGeneralImages(id));
            List<com.nikyfarm.dto.ProductVariantResponseDto> variants =
                    productVariantService.getVariantsByProduct(id);
            model.addAttribute("variants", variants);
            model.addAttribute("attributeValues", productAttributeValueService.getValuesByProduct(id));
            List<Map<String, Object>> variantImageGroups = variants.stream().map(variant -> {
                Map<String, Object> group = new LinkedHashMap<>();
                group.put("variant", variant);
                group.put("images", productImageService.getImagesByVariant(id, variant.id()));
                return group;
            }).toList();
            model.addAttribute("variantImageGroups", variantImageGroups);
            return "admin/products/form";
        }
        try {
            productService.updateProduct(id, product);
            redirectAttributes.addFlashAttribute("successMessage", "Product details updated successfully.");
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildEditUrl(id, returnPage, search);
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildListUrl(page, search);
    }

    private void addFormReferenceData(
            Model model, String formAction, boolean isEdit, int page, String search) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("formAction", formAction);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("returnPage", Math.max(page, 0));
        model.addAttribute("search", search == null ? "" : search.trim());
        model.addAttribute("listReturnUrl", buildListUrl(page, search));
    }

    private String buildListUrl(int page, String search) {
        String query = search == null ? "" : search.trim();
        String url = "/admin/products?page=" + Math.max(page, 0);
        return query.isBlank() ? url : url + "&search=" + encodeQuery(query);
    }

    private String buildEditUrl(Long id, int page, String search) {
        String url = "/admin/products/" + id + "/edit?page=" + Math.max(page, 0);
        String query = search == null ? "" : search.trim();
        return query.isBlank() ? url : url + "&search=" + encodeQuery(query);
    }

    private String encodeQuery(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
