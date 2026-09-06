package com.nikyfarm.controller.admin;

import com.nikyfarm.service.CategoryService;
import com.nikyfarm.service.ProductService;
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

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("categoryCount", categoryService.getAllCategories().size());
        model.addAttribute("productCount", productService.getProducts(null, PageRequest.of(0, 1)).getTotalElements());
        return "admin/index";
    }
}
