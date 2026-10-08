package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.domain.Slugs;
import com.glowvera.dto.request.AdminListQuery;
import com.glowvera.dto.request.CreateProductRequest;
import com.glowvera.dto.request.CreateVariantRequest;
import com.glowvera.dto.request.UpdateProductRequest;
import com.glowvera.dto.request.UpdateVariantRequest;
import com.glowvera.dto.response.AdminProductViews;
import com.glowvera.dto.response.PageResponse;
import com.glowvera.dto.response.Pagination;
import com.glowvera.entity.Category;
import com.glowvera.entity.Concern;
import com.glowvera.entity.Product;
import com.glowvera.entity.ProductVariant;
import com.glowvera.entity.SkinType;
import com.glowvera.mapper.ProductMapper;
import com.glowvera.mapper.StockMapper;
import com.glowvera.repository.CategoryRepository;
import com.glowvera.repository.ConcernRepository;
import com.glowvera.repository.ProductRepository;
import com.glowvera.repository.ProductSpecifications;
import com.glowvera.repository.ProductVariantRepository;
import com.glowvera.repository.SkinTypeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminProductService {

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final SkinTypeRepository skinTypes;
    private final ConcernRepository concerns;
    private final Clock clock;

    public AdminProductService(ProductRepository products, ProductVariantRepository variants,
                               CategoryRepository categories, SkinTypeRepository skinTypes,
                               ConcernRepository concerns, Clock clock) {
        this.products = products;
        this.variants = variants;
        this.categories = categories;
        this.skinTypes = skinTypes;
        this.concerns = concerns;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductViews.ListItem> list(AdminListQuery q) {
        PageRequest page = PageRequest.of(q.page() - 1, q.limit(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Product> result = products.findAll(ProductSpecifications.adminList(q.q(), q.status()), page);
        return new PageResponse<>(
                result.getContent().stream().map(ProductMapper::toAdminListItem).toList(),
                Pagination.of(q.page(), q.limit(), result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public AdminProductViews.Detail getById(Long id) {
        return ProductMapper.toAdminDetail(find(id), LocalDate.now(clock));
    }

    public AdminProductViews.Detail create(CreateProductRequest r) {
        Product p = new Product();
        p.setName(r.name());
        p.setSlug(uniqueSlug(r.name()));
        p.setBrand(r.brand());
        p.setDescription(r.description());
        p.setIngredients(r.ingredients());
        p.setHowToUse(r.howToUse());
        p.setImageUrl(r.imageUrl());
        p.setActive(r.isActive() == null || r.isActive());
        p.setCategory(category(r.categoryId()));
        p.setSkinTypes(skinTypesByIds(r.skinTypeIds()));
        p.setConcerns(concernsByIds(r.concernIds()));
        Product saved = products.saveAndFlush(p);
        return ProductMapper.toAdminDetail(saved, LocalDate.now(clock));
    }

    public AdminProductViews.Detail update(Long id, UpdateProductRequest r) {
        if (r.isEmpty()) {
            throw AppException.badRequest("Provide at least one field to update");
        }
        Product p = find(id);

        if (r.name() != null) p.setName(r.name());          // the slug is never touched, so URLs stay stable
        if (r.brand() != null) p.setBrand(blankToNull(r.brand()));
        if (r.description() != null) p.setDescription(r.description());
        if (r.ingredients() != null) p.setIngredients(blankToNull(r.ingredients()));
        if (r.howToUse() != null) p.setHowToUse(blankToNull(r.howToUse()));
        if (r.imageUrl() != null) p.setImageUrl(blankToNull(r.imageUrl()));
        if (r.categoryId() != null) p.setCategory(category(r.categoryId()));
        if (r.skinTypeIds() != null) p.setSkinTypes(skinTypesByIds(r.skinTypeIds()));
        if (r.concernIds() != null) p.setConcerns(concernsByIds(r.concernIds()));
        if (r.isActive() != null) p.setActive(r.isActive());

        products.saveAndFlush(p);
        return ProductMapper.toAdminDetail(p, LocalDate.now(clock));
    }

    public AdminProductViews.VariantView createVariant(Long productId, CreateVariantRequest r) {
        Product product = find(productId);
        if (variants.findBySku(r.sku()).isPresent()) {
            throw AppException.conflict("A variant with this SKU already exists");
        }
        ProductVariant v = new ProductVariant();
        v.setProduct(product);
        v.setSku(r.sku());
        v.setName(r.name());
        v.setPriceCents(r.priceCents());
        v.setLowStockThreshold(r.lowStockThreshold() == null ? 5 : r.lowStockThreshold());
        // stock starts at 0: it only ever arrives through batches
        return StockMapper.toVariantView(variants.saveAndFlush(v));
    }

    public AdminProductViews.VariantView updateVariant(Long id, UpdateVariantRequest r) {
        if (r.isEmpty()) {
            throw AppException.badRequest("Provide at least one field to update");
        }
        ProductVariant v = variants.findById(id).orElseThrow(() -> AppException.notFound("Variant not found"));
        if (r.name() != null) v.setName(r.name());
        if (r.priceCents() != null) v.setPriceCents(r.priceCents());
        if (r.lowStockThreshold() != null) v.setLowStockThreshold(r.lowStockThreshold());
        if (r.isActive() != null) v.setActive(r.isActive());
        return StockMapper.toVariantView(variants.saveAndFlush(v));
    }

    // ── helpers ──

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> AppException.notFound("Product not found"));
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> AppException.badRequest("Category does not exist"));
    }

    private Set<SkinType> skinTypesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> unique = new HashSet<>(ids);
        List<SkinType> found = skinTypes.findAllById(unique);
        if (found.size() != unique.size()) {
            throw AppException.badRequest("One or more skin types do not exist");
        }
        return new HashSet<>(found);
    }

    private Set<Concern> concernsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<Long> unique = new HashSet<>(ids);
        List<Concern> found = concerns.findAllById(unique);
        if (found.size() != unique.size()) {
            throw AppException.badRequest("One or more concerns do not exist");
        }
        return new HashSet<>(found);
    }

    private String uniqueSlug(String name) {
        String base = Slugs.slugify(name);
        String slug = base;
        int n = 2;
        while (products.existsBySlug(slug)) {
            slug = base + "-" + n++;
        }
        return slug;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
