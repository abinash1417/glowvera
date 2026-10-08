package com.glowvera.web;

import com.glowvera.dto.request.ProductListQuery;
import com.glowvera.dto.response.PageResponse;
import com.glowvera.dto.response.PublicProductViews;
import com.glowvera.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    public ApiResponse<PageResponse<PublicProductViews.ListItem>> list(@Valid ProductListQuery query) {
        return ApiResponse.ok(products.list(query));
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicProductViews.Detail> get(
            @PathVariable @Pattern(regexp = "^[a-z0-9-]+$", message = "Invalid slug") String slug) {
        return ApiResponse.ok(products.getBySlug(slug));
    }
}
