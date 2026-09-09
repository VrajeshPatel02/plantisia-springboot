package com.nikyfarm.controller.admin;

import com.nikyfarm.dto.OrderResponseDto;
import com.nikyfarm.dto.OrderStatusUpdateDto;
import com.nikyfarm.entity.Order.OrderStatus;
import com.nikyfarm.entity.Order.PaymentStatus;
import com.nikyfarm.exception.BusinessRuleViolationException;
import com.nikyfarm.exception.ResourceNotFoundException;
import com.nikyfarm.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<OrderResponseDto> orderPage = orderService.getAllOrders(status, PageRequest.of(page, size));

        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", OrderStatus.values());

        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String viewOrderDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            OrderResponseDto order = orderService.getOrderById(id,null, true );
            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", OrderStatus.values());
            return "admin/orders/detail";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorM essage", e.getMessage());
            return "redirect:/admin/orders";
        }
    }

    @PostMapping("/{id}/status")
    public String updateOrderStatus(
            @PathVariable Long id,
            @ModelAttribute OrderStatusUpdateDto updateDto,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, updateDto);
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully");
        } catch (ResourceNotFoundException | BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id, null, true);
            redirectAttributes.addFlashAttribute("successMessage", "Order cancelled and stock restored successfully");
        } catch (ResourceNotFoundException | BusinessRuleViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}