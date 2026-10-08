package com.glowvera.mapper;

import com.glowvera.domain.StockMath;
import com.glowvera.dto.response.AdminProductViews;
import com.glowvera.dto.response.PublicProductViews;
import com.glowvera.dto.response.Refs;
import com.glowvera.entity.Concern;
import com.glowvera.entity.Product;
import com.glowvera.entity.ProductVariant;
import com.glowvera.entity.SkinType;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    // public (customer)

    private static List<PublicProductViews.Variant> publicVariants(Product p) {
        return p.getVariants().stream()
                .filter(ProductVariant::isActive)
                .sorted(Comparator.comparingLong(ProductVariant::getPriceCents).thenComparing(ProductVariant::getId))
                .map(v -> new PublicProductViews.Variant(
                        v.getId(), v.getSku(), v.getName(), v.getPriceCents(), v.availableQty()))
                .toList();
    }

    private static List<String> skinTypeNames(Product p) {
        return p.getSkinTypes().stream().map(SkinType::getName).sorted().toList();
    }

    private static List<String> concernNames(Product p) {
        return p.getConcerns().stream().map(Concern::getName).sorted().toList();
    }

    public static PublicProductViews.ListItem toPublicListItem(Product p) {
        List<PublicProductViews.Variant> variants = publicVariants(p);
        long priceFrom = variants.stream().mapToLong(PublicProductViews.Variant::priceCents).min().orElse(0);
        return new PublicProductViews.ListItem(
                p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getImageUrl(),
                new Refs.PublicCategory(p.getCategory().getName(), p.getCategory().getSlug()),
                skinTypeNames(p), concernNames(p), priceFrom,
                variants.stream().anyMatch(v -> v.available() > 0), variants.size(), p.getCreatedAt());
    }

    public static PublicProductViews.Detail toPublicDetail(Product p) {
        List<PublicProductViews.Variant> variants = publicVariants(p);
        return new PublicProductViews.Detail(
                p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getDescription(),
                p.getIngredients(), p.getHowToUse(), p.getImageUrl(),
                new Refs.PublicCategory(p.getCategory().getName(), p.getCategory().getSlug()),
                skinTypeNames(p), concernNames(p), variants,
                variants.stream().anyMatch(v -> v.available() > 0));
    }

    // admin

    private static Refs.CategoryRef categoryRef(Product p) {
        return new Refs.CategoryRef(p.getCategory().getId(), p.getCategory().getName(), p.getCategory().getSlug());
    }

    public static AdminProductViews.ListItem toAdminListItem(Product p) {
        List<AdminProductViews.VariantView> variants = p.getVariants().stream()
                .sorted(Comparator.comparing(ProductVariant::getId))
                .map(StockMapper::toVariantView)
                .toList();
        int totalAvailable = variants.stream()
                .filter(AdminProductViews.VariantView::isActive)
                .mapToInt(AdminProductViews.VariantView::available)
                .sum();
        return new AdminProductViews.ListItem(
                p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getImageUrl(), p.isActive(),
                categoryRef(p), p.getCreatedAt(), variants, totalAvailable);
    }

    public static AdminProductViews.Detail toAdminDetail(Product p, LocalDate today) {
        List<AdminProductViews.VariantDetail> variants = p.getVariants().stream()
                .sorted(Comparator.comparing(ProductVariant::getId))
                .map(v -> {
                    AdminProductViews.VariantView base = StockMapper.toVariantView(v);
                    List<AdminProductViews.BatchView> batches = v.getBatches().stream()
                            .sorted(Comparator.comparing(com.glowvera.entity.StockBatch::getExpiryDate))
                            .map(b -> StockMapper.toBatchView(b, today))
                            .toList();
                    return new AdminProductViews.VariantDetail(
                            base.id(), base.productId(), base.sku(), base.name(), base.priceCents(),
                            base.stockQty(), base.reservedQty(), base.available(), base.lowStockThreshold(),
                            base.isActive(), base.stockStatus(), batches);
                })
                .toList();
        return new AdminProductViews.Detail(
                p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getDescription(),
                p.getIngredients(), p.getHowToUse(), p.getImageUrl(), p.isActive(), categoryRef(p),
                p.getSkinTypes().stream().sorted(Comparator.comparing(SkinType::getName))
                        .map(s -> new Refs.NameRef(s.getId(), s.getName())).toList(),
                p.getConcerns().stream().sorted(Comparator.comparing(Concern::getName))
                        .map(c -> new Refs.NameRef(c.getId(), c.getName())).toList(),
                p.getCreatedAt(), p.getUpdatedAt(), variants);
    }
}
