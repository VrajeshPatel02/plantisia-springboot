package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.ReviewResponseDto;
import com.nikyfarm.dto.UserResponseDto;
import com.nikyfarm.entity.Order.OrderStatus;
import com.nikyfarm.service.AddressesService;
import com.nikyfarm.service.CartService;
import com.nikyfarm.service.CategoryService;
import com.nikyfarm.service.CouponService;
import com.nikyfarm.service.OrderService;
import com.nikyfarm.service.ProductService;
import com.nikyfarm.service.ReviewService;
import com.nikyfarm.service.UserService;
import com.nikyfarm.service.WishlistService;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final CouponService couponService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final AddressesService addressesService;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        var categories = categoryService.getAllCategories();
        var productsPage = productService.getProducts(null, PageRequest.of(0, 1));
        var ordersPage = orderService.getAllOrders(null, PageRequest.of(0, 1));
        List<UserResponseDto> users = userService.getAll();
        List<ReviewResponseDto> reviews = reviewService.getAllReviews();
        var couponsPage = couponService.getAllCoupons(PageRequest.of(0, 1));
        var addressesPage = addressesService.getAllAddresses(PageRequest.of(0, 1));

        long ratedReviewCount = reviews.stream().filter(review -> review.rating() != null).count();
        double averageRating = reviews.stream()
                .filter(review -> review.rating() != null)
                .mapToInt(ReviewResponseDto::rating)
                .average()
                .orElse(0.0);

        List<String> orderStatusLabels = Arrays.stream(OrderStatus.values())
                .map(Enum::name)
                .toList();
        List<Long> orderStatusCounts = Arrays.stream(OrderStatus.values())
                .map(status -> orderService.getAllOrders(status, PageRequest.of(0, 1)).getTotalElements())
                .toList();
        List<Long> reviewRatingCounts = List.of(1, 2, 3, 4, 5).stream()
                .map(rating -> reviews.stream()
                        .filter(review -> review.rating() != null && review.rating().intValue() == rating)
                        .count())
                .toList();

        model.addAttribute("categoryCount", categories.size());
        model.addAttribute("productCount", productsPage.getTotalElements());
        model.addAttribute("orderCount", ordersPage.getTotalElements());
        model.addAttribute("userCount", users.size());
        model.addAttribute("activeUserCount", users.stream().filter(UserResponseDto::isActive).count());
        model.addAttribute("reviewCount", reviews.size());
        model.addAttribute("ratedReviewCount", ratedReviewCount);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("couponCount", couponsPage.getTotalElements());
        model.addAttribute("cartItemCount", cartService.listAllItems(null).size());
        model.addAttribute("wishlistItemCount", wishlistService.getAll(null).size());
        model.addAttribute("addressCount", addressesPage.getTotalElements());
        model.addAttribute("orderStatusLabels", orderStatusLabels);
        model.addAttribute("orderStatusCounts", orderStatusCounts);
        model.addAttribute("reviewRatingCounts", reviewRatingCounts);

        return "admin/index";
    }
}
