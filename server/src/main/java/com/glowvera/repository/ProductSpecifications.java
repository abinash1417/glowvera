package com.glowvera.repository;

import com.glowvera.entity.Product;
import com.glowvera.entity.ProductVariant;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public record PublicFilter(String q, String category, String skinType, String concern,
                               Long minPrice, Long maxPrice) {
    }

    public static Specification<Product> publicCatalog(PublicFilter f) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isTrue(root.get("active")));

            Subquery<Long> variants = query.subquery(Long.class);
            Root<ProductVariant> v = variants.from(ProductVariant.class);
            List<Predicate> vp = new ArrayList<>();
            vp.add(cb.equal(v.get("product"), root));
            vp.add(cb.isTrue(v.get("active")));
            if (f.minPrice() != null) {
                vp.add(cb.greaterThanOrEqualTo(v.<Long>get("priceCents"), f.minPrice()));
            }
            if (f.maxPrice() != null) {
                vp.add(cb.lessThanOrEqualTo(v.<Long>get("priceCents"), f.maxPrice()));
            }
            variants.select(v.<Long>get("id")).where(vp.toArray(new Predicate[0]));
            all.add(cb.exists(variants));

            if (hasText(f.q())) {
                String pattern = like(f.q());
                all.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("brand")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\')));
            }
            if (hasText(f.category())) {
                all.add(cb.equal(root.join("category").get("slug"), f.category()));
            }
            if (hasText(f.skinType())) {
                all.add(cb.equal(root.join("skinTypes").get("name"), f.skinType()));
            }
            if (hasText(f.concern())) {
                all.add(cb.equal(root.join("concerns").get("name"), f.concern()));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> adminList(String q, String status) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if ("active".equals(status)) {
                all.add(cb.isTrue(root.get("active")));
            } else if ("inactive".equals(status)) {
                all.add(cb.isFalse(root.get("active")));
            }
            if (hasText(q)) {
                String pattern = like(q);
                all.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("brand")), pattern, '\\')));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
    }

    static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    static String like(String text) {
        String escaped = text.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
