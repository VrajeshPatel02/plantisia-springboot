package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.ReviewResponseDto;
import com.nikyfarm.dto.ReviewUpdateRequestDto;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.DuplicateResourceException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Server-rendered admin moderation UI for Reviews. No standalone ReviewImage screen --
 * its thumbnail and delete action live inline on the review it belongs to, since an
 * image has no independent browse use case. Admin edits bypass the ownership check
 * that a customer editing their own review is subject to (see ReviewServiceImpl).
 */
@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("reviews", reviewService.getAllReviews());
        return "admin/reviews/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ReviewResponseDto existing = reviewService.getById(id);
        ReviewUpdateRequestDto dto = new ReviewUpdateRequestDto();
        dto.setRating(existing.rating());
        dto.setTitle(existing.title());
        dto.setComment(existing.comment());
        dto.setReviewImageId(existing.reviewImageId());
        dto.setIsVerifiedPurchase(existing.isVerifiedPurchase());
        model.addAttribute("review", dto);
        model.addAttribute("existing", existing);
        model.addAttribute("reviewId", id);
        return "admin/reviews/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("review") ReviewUpdateRequestDto request,
                          BindingResult bindingResult,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("existing", reviewService.getById(id));
            model.addAttribute("reviewId", id);
            return "admin/reviews/form";
        }
        try {
            reviewService.updateReview(id, null, true, request);
            redirectAttributes.addFlashAttribute("successMessage", "Review updated successfully.");
            return "redirect:/admin/reviews";
        } catch (DuplicateResourceException | BusinessRuleViolationException | ResourceNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("existing", reviewService.getById(id));
            model.addAttribute("reviewId", id);
            return "admin/reviews/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.deleteReview(id, null, true);
            redirectAttributes.addFlashAttribute("successMessage", "Review deleted successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/remove-image")
    public String removeImage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewService.removeImage(id);
            redirectAttributes.addFlashAttribute("successMessage", "Image removed successfully.");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/reviews/" + id + "/edit";
    }
}
