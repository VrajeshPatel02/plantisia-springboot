package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.BulkImageUploadResponse;
import com.nikyfarm.exception.ImageUploadException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.ProductImageService;
import com.nikyfarm.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin UI for a product's general (not variant-scoped) image gallery.
 * Variant-specific images are managed on their own page — see {@link AdminVariantImageController} —
 * since most products only really have one variant and managing every variant's photos from one
 * combined screen isn't useful.
 */
@Controller
@RequestMapping("/admin/products/{productId}/images")
@RequiredArgsConstructor
public class AdminProductImageController {

    private final ProductImageService productImageService;
    private final ProductService productService;

    @GetMapping
    public String list(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.getProductById(productId));
        model.addAttribute("images", productImageService.getGeneralImages(productId));
        return "admin/images/list";
    }

    @PostMapping
    public String upload(
            @PathVariable Long productId,
            @RequestParam("files") List<MultipartFile> files,
            RedirectAttributes redirectAttributes) {
        List<MultipartFile> actual = files.stream().filter(f -> !f.isEmpty()).toList();
        if (actual.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Select at least one file to upload.");
            return "redirect:/admin/products/" + productId + "/images";
        }
        try {
            BulkImageUploadResponse result = productImageService.uploadProductImagesBulk(productId, actual);
            String message = result.getSuccessCount()
                    + " image(s) uploaded successfully"
                    + (result.getFailureCount() > 0 ? "; " + result.getFailureCount() + " failed." : ".");
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (ResourceNotFoundException | ImageUploadException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/images";
    }

    @PostMapping("/{imageId}/replace")
    public String replace(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            productImageService.replaceProductImage(productId, imageId, file);
            redirectAttributes.addFlashAttribute("successMessage", "Image replaced successfully.");
        } catch (ResourceNotFoundException | ImageUploadException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/images";
    }

    @PostMapping("/{imageId}/delete")
    public String delete(
            @PathVariable Long productId, @PathVariable Long imageId, RedirectAttributes redirectAttributes) {
        try {
            productImageService.deleteProductImage(productId, imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Image deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products/" + productId + "/images";
    }
}
