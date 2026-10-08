package com.glowvera.web;

import com.glowvera.dto.request.AdminListQuery;
import com.glowvera.dto.request.CreateBatchRequest;
import com.glowvera.dto.request.CreateProductRequest;
import com.glowvera.dto.request.CreateVariantRequest;
import com.glowvera.dto.request.UpdateProductRequest;
import com.glowvera.dto.request.UpdateVariantRequest;
import com.glowvera.dto.response.AdminProductViews;
import com.glowvera.dto.response.InventoryViews;
import com.glowvera.dto.response.PageResponse;
import com.glowvera.service.AdminProductService;
import com.glowvera.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminProductController {

    private final AdminProductService products;
    private final InventoryService inventory;

    public AdminProductController(AdminProductService products, InventoryService inventory) {
        this.products = products;
        this.inventory = inventory;
    }

    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.ok(Map.of("message", "Hello admin"));
    }

    //  products
    @GetMapping("/products")
    public ApiResponse<PageResponse<AdminProductViews.ListItem>> list(@Valid AdminListQuery query) {
        return ApiResponse.ok(products.list(query));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<AdminProductViews.Detail> get(@PathVariable Long id) {
        return ApiResponse.ok(products.getById(id));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<AdminProductViews.Detail>> create(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(products.create(request)));
    }

    @PatchMapping("/products/{id}")
    public ApiResponse<AdminProductViews.Detail> update(
            @PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.ok(products.update(id, request));
    }

    //  variants
    @PostMapping("/products/{id}/variants")
    public ResponseEntity<ApiResponse<AdminProductViews.VariantView>> createVariant(
            @PathVariable Long id, @Valid @RequestBody CreateVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(products.createVariant(id, request)));
    }

    @PatchMapping("/variants/{id}")
    public ApiResponse<AdminProductViews.VariantView> updateVariant(
            @PathVariable Long id, @Valid @RequestBody UpdateVariantRequest request) {
        return ApiResponse.ok(products.updateVariant(id, request));
    }

    //  stock batches
    @PostMapping("/variants/{id}/batches")
    public ResponseEntity<ApiResponse<InventoryViews.ReceiveBatchResult>> receiveBatch(
            @PathVariable Long id, @Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(inventory.receiveBatch(id, request)));
    }

    @PostMapping("/batches/{id}/write-off")
    public ApiResponse<InventoryViews.WriteOffResult> writeOff(@PathVariable Long id) {
        return ApiResponse.ok(inventory.writeOff(id));
    }

    @GetMapping("/inventory/alerts")
    public ApiResponse<InventoryViews.Alerts> alerts(
            @RequestParam(defaultValue = "60") @Min(1) @Max(365) int days) {
        return ApiResponse.ok(inventory.getAlerts(days));
    }
}
