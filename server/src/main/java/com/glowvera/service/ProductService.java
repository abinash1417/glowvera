package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.dto.request.ProductListQuery;
import com.glowvera.dto.response.PageResponse;
import com.glowvera.dto.response.Pagination;
import com.glowvera.dto.response.PublicProductViews;
import com.glowvera.mapper.ProductMapper;
import com.glowvera.repository.ProductRepository;
import com.glowvera.repository.ProductSpecifications;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Map<String, Comparator<PublicProductViews.ListItem>> SORTERS = Map.of(
            "newest", Comparator.comparing(PublicProductViews.ListItem::createdAt).reversed(),
            "name", Comparator.comparing(PublicProductViews.ListItem::name, String.CASE_INSENSITIVE_ORDER),
            "price_asc", Comparator.comparingLong(PublicProductViews.ListItem::priceFromCents),
            "price_desc", Comparator.comparingLong(PublicProductViews.ListItem::priceFromCents).reversed());

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public PageResponse<PublicProductViews.ListItem> list(ProductListQuery q) {
        if (q.minPrice() != null && q.maxPrice() != null && q.minPrice() > q.maxPrice()) {
            throw AppException.badRequest("minPrice cannot be greater than maxPrice");
        }

        // The sort key "price from" is computed from variants, so the (small) filtered set is sorted in memory.
        // For a very large catalog: add a denormalised min_price column and page in SQL instead.
        List<PublicProductViews.ListItem> all = products.findAll(ProductSpecifications.publicCatalog(
                        new ProductSpecifications.PublicFilter(
                                q.q(), q.category(), q.skinType(), q.concern(), q.minPrice(), q.maxPrice())))
                .stream()
                .map(ProductMapper::toPublicListItem)
                .sorted(SORTERS.get(q.sort()))
                .toList();

        int from = Math.min((q.page() - 1) * q.limit(), all.size());
        int to = Math.min(from + q.limit(), all.size());
        return new PageResponse<>(all.subList(from, to), Pagination.of(q.page(), q.limit(), all.size()));
    }

    public PublicProductViews.Detail getBySlug(String slug) {
        return products.findBySlugAndActiveTrue(slug)
                .map(ProductMapper::toPublicDetail)
                .orElseThrow(() -> AppException.notFound("Product not found"));
    }
}
