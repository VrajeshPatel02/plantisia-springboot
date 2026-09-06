package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.BulkImageUploadResponse;
import com.nikyfarm.exception.ImageUploadException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.ProductImageService;
import com.nikyfarm.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Server-rendered admin UI for a single variant's images — kept separate from the
 * general product gallery ({@link AdminProductImageController}) so a variant's photos
 * live alongside the variant itself instead of behind a "which variant?" dropdown on a
 * combined all-images screen.
 */
@Controller
@RequestMapping("/admin/products/{productId}/variants/{variantId}/images")
@RequiredArgsConstructor
public class AdminVariantImageController {

    private final ProductImageService productImageService;
    private final ProductVariantService productVariantService;

    @GetMapping
    public String list(@PathVariable Long productId, @PathVariable Long variantId, Model model) {
        model.addAttribute("productId", productId);
        model.addAttribute("variant", productVariantService.getVariantById(productId, variantId));
        model.addAttribute("images", productImageService.getImagesByVariant(productId, variantId));
        return "admin/images/variant-list";
    }

    @PostMapping
    public String upload(@PathVariable Long productId,
                          @PathVariable Long variantId,
                          @RequestParam("files") List<MultipartFile> files,
                          RedirectAttributes redirectAttributes) {
        List<MultipartFile> actual = files.stream().filter(f -> !f.isEmpty()).toList();
        if (actual.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Select at least one file to upload.");
            return redirectToList(productId, variantId);
        }
        try {
            BulkImageUploadResponse result = productImageService.uploadVariantImagesBulk(productId, variantId, actual);
            String message = result.getSuccessCount() + " image(s) uploaded successfully"
                    + (result.getFailureCount() > 0 ? "; " + result.getFailureCount() + " failed." : ".");
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (ResourceNotFoundException | ImageUploadException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToList(productId, variantId);
    }

    @PostMapping("/{imageId}/replace")
    public String replace(@PathVariable Long productId,
                           @PathVariable Long variantId,
                           @PathVariable Long imageId,
                           @RequestParam("file") MultipartFile file,
                           RedirectAttributes redirectAttributes) {
        try {
            productImageService.replaceProductImage(productId, imageId, file);
            redirectAttributes.addFlashAttribute("successMessage", "Image replaced successfully.");
        } catch (ResourceNotFoundException | ImageUploadException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToList(productId, variantId);
    }

    @PostMapping("/{imageId}/delete")
    public String delete(@PathVariable Long productId,
                          @PathVariable Long variantId,
                          @PathVariable Long imageId,
                          RedirectAttributes redirectAttributes) {
        try {
            productImageService.deleteProductImage(productId, imageId);
            redirectAttributes.addFlashAttribute("successMessage", "Image deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirectToList(productId, variantId);
    }

    private String redirectToList(Long productId, Long variantId) {
        return "redirect:/admin/products/" + productId + "/variants/" + variantId + "/images";
    }
}
