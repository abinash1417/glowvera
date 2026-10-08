package com.glowvera.web;

import com.glowvera.dto.response.FiltersResponse;
import com.glowvera.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/filters")
    public ApiResponse<FiltersResponse> filters() {
        return ApiResponse.ok(catalog.getFilters());
    }
}
