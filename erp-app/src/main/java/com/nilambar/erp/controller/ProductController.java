package com.nilambar.erp.controller;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.service.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ErpProperties properties;

    public ProductController(ProductRepository productRepository, ErpProperties properties) {
        this.productRepository = productRepository;
        this.properties = properties;
    }

    @GetMapping
    public String list(@RequestParam(value = "category", required = false) String category,
                       @RequestParam(value = "q", required = false) String keyword,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        String normalisedCategory = (category == null || category.isBlank()) ? null : category;
        String normalisedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        Page<Product> products = productRepository.search(normalisedCategory, normalisedKeyword,
                PageRequest.of(Math.max(page, 0), properties.getCatalog().getPageSize(), Sort.by("name")));

        model.addAttribute("products", products);
        model.addAttribute("categories", productRepository.findCategories());
        model.addAttribute("selectedCategory", normalisedCategory);
        model.addAttribute("keyword", normalisedKeyword);
        return "product/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found."));
        model.addAttribute("product", product);
        return "product/detail";
    }
}
