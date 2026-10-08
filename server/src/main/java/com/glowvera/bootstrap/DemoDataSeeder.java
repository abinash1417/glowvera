package com.glowvera.bootstrap;

import com.glowvera.config.AppProperties;
import com.glowvera.entity.Product;
import com.glowvera.entity.ProductVariant;
import com.glowvera.entity.StockBatch;
import com.glowvera.repository.CategoryRepository;
import com.glowvera.repository.ConcernRepository;
import com.glowvera.repository.ProductRepository;
import com.glowvera.repository.ProductVariantRepository;
import com.glowvera.repository.SkinTypeRepository;
import com.glowvera.repository.StockBatchRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private record Batch(String code, int quantity, int daysUntilExpiry) {
    }

    private record Var(String sku, String name, long rupees, int lowStock, List<Batch> batches) {
        Var(String sku, String name, long rupees, List<Batch> batches) {
            this(sku, name, rupees, 5, batches);
        }
    }

    private record Item(String name, String slug, String category, List<String> skinTypes, List<String> concerns,
                        String description, String ingredients, String howToUse, List<Var> variants) {
    }

    private static final List<Item> CATALOG = List.of(
            new Item("Hydra Glow Gel Moisturizer", "hydra-glow-gel-moisturizer", "skincare",
                    List.of("Oily", "Combination", "Normal"), List.of("Hydration"),
                    "A lightweight, oil-free gel that melts into skin and keeps it hydrated all day without a greasy feel.",
                    "Aqua, Glycerin, Hyaluronic Acid, Aloe Barbadensis Leaf Juice, Niacinamide, Panthenol",
                    "Apply a pea-sized amount to clean face morning and night.",
                    List.of(new Var("HGM-50", "50ml", 1850, List.of(new Batch("HGM-A1", 40, 400))),
                            new Var("HGM-100", "100ml", 3200, List.of(new Batch("HGM-B1", 25, 420))))),
            new Item("Vitamin C Brightening Serum", "vitamin-c-brightening-serum", "skincare",
                    List.of("Normal", "Dry", "Combination"), List.of("Brightening", "Dark Spots"),
                    "A 10% Vitamin C serum that visibly evens skin tone and fades dark spots.",
                    "Aqua, Ascorbic Acid, Ferulic Acid, Vitamin E, Hyaluronic Acid",
                    "Use 3 to 4 drops on clean skin in the morning, followed by sunscreen.",
                    List.of(// two batches: one expires soon, to demo the near-expiry alert and FEFO selling
                            new Var("VCS-15", "15ml", 2900,
                                    List.of(new Batch("VCS-A1", 6, 25), new Batch("VCS-A2", 30, 300))),
                            new Var("VCS-30", "30ml", 4900, List.of(new Batch("VCS-B1", 20, 330))))),
            new Item("Gentle Foam Cleanser", "gentle-foam-cleanser", "skincare",
                    List.of("Sensitive", "Dry", "Normal"), List.of("Hydration"),
                    "A soap-free, fragrance-free cleanser that removes dirt without stripping the skin barrier.",
                    "Aqua, Coco-Glucoside, Glycerin, Chamomile Extract, Allantoin",
                    "Massage onto damp skin, then rinse with lukewarm water.",
                    List.of(new Var("GFC-100", "100ml", 1450, List.of(new Batch("GFC-A1", 60, 500))),
                            new Var("GFC-200", "200ml", 2450, List.of(new Batch("GFC-B1", 35, 520))))),
            new Item("Clear Pore Salicylic Cleanser", "clear-pore-salicylic-cleanser", "skincare",
                    List.of("Oily", "Combination"), List.of("Acne"),
                    "A 2% salicylic acid cleanser that unclogs pores and helps control breakouts.",
                    "Aqua, Salicylic Acid, Zinc PCA, Tea Tree Leaf Oil, Glycerin",
                    "Use once or twice daily. Avoid the eye area. Contains salicylic acid.",
                    List.of(new Var("CPS-100", "100ml", 1650, List.of(new Batch("CPS-A1", 45, 450))))),
            new Item("SPF 50 Daily Sunscreen", "spf-50-daily-sunscreen", "skincare",
                    List.of("Oily", "Dry", "Combination", "Sensitive", "Normal"), List.of("Sun Protection"),
                    "Broad-spectrum SPF 50 with a weightless finish that leaves no white cast.",
                    "Aqua, Zinc Oxide, Niacinamide, Tocopherol, Aloe Vera",
                    "Apply generously 15 minutes before sun exposure. Reapply every 2 hours.",
                    List.of(new Var("SPF-50", "50ml", 2200, List.of(new Batch("SPF-A1", 70, 600))))),
            new Item("Retinol Night Cream", "retinol-night-cream", "skincare",
                    List.of("Normal", "Dry"), List.of("Anti-aging"),
                    "A rich night cream with encapsulated retinol that smooths fine lines.",
                    "Aqua, Retinol, Shea Butter, Squalane, Ceramide NP",
                    "Use at night, 2 to 3 times a week at first. Always use sunscreen the next day. Not for use in pregnancy.",
                    List.of(// low stock on purpose (threshold 5), to demo the low-stock alert
                            new Var("RNC-30", "30ml", 3800, 5, List.of(new Batch("RNC-A1", 3, 280))))),
            new Item("Argan Oil Repair Shampoo", "argan-oil-repair-shampoo", "hair-care",
                    List.of(), List.of("Hair Fall"),
                    "A sulfate-free shampoo with argan oil that strengthens weak, damaged hair.",
                    "Aqua, Argania Spinosa Kernel Oil, Biotin, Keratin, Panthenol",
                    "Massage into wet hair and scalp, then rinse thoroughly.",
                    List.of(new Var("ARS-250", "250ml", 1950, List.of(new Batch("ARS-A1", 50, 540))),
                            new Var("ARS-500", "500ml", 3400, List.of(new Batch("ARS-B1", 30, 560))))),
            new Item("Shea Butter Body Lotion", "shea-butter-body-lotion", "body-care",
                    List.of("Dry", "Sensitive", "Normal"), List.of("Hydration"),
                    "A creamy daily lotion with shea butter for soft skin that stays moisturised for 24 hours.",
                    "Aqua, Butyrospermum Parkii Butter, Glycerin, Cocoa Butter, Vitamin E",
                    "Apply all over the body after bathing.",
                    List.of(new Var("SBL-200", "200ml", 1600, List.of(new Batch("SBL-A1", 55, 480))),
                            new Var("SBL-400", "400ml", 2800, List.of(new Batch("SBL-B1", 28, 500))))),
            new Item("Matte Velvet Lipstick", "matte-velvet-lipstick", "makeup",
                    List.of(), List.of(),
                    "A comfortable, long-wearing matte lipstick in three flattering shades.",
                    "Ricinus Communis Seed Oil, Candelilla Wax, Vitamin E, Mica, Iron Oxides",
                    "Apply directly from the bullet or with a lip brush.",
                    List.of(new Var("MVL-ROSE", "Rose Petal", 1950, List.of(new Batch("MVL-R1", 24, 700))),
                            // out of stock on purpose, to demo the sold-out state
                            new Var("MVL-RUBY", "Ruby Red", 1950, List.of()),
                            new Var("MVL-NUDE", "Nude Beige", 1950, List.of(new Batch("MVL-N1", 18, 700))))));

    private final AppProperties props;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final StockBatchRepository batches;
    private final CategoryRepository categories;
    private final SkinTypeRepository skinTypes;
    private final ConcernRepository concerns;
    private final Clock clock;

    public DemoDataSeeder(AppProperties props, ProductRepository products, ProductVariantRepository variants,
                          StockBatchRepository batches, CategoryRepository categories,
                          SkinTypeRepository skinTypes, ConcernRepository concerns, Clock clock) {
        this.props = props;
        this.products = products;
        this.variants = variants;
        this.batches = batches;
        this.categories = categories;
        this.skinTypes = skinTypes;
        this.concerns = concerns;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seedDemoData() || products.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);

        for (Item item : CATALOG) {
            Product p = new Product();
            p.setName(item.name());
            p.setSlug(item.slug());
            p.setBrand("Glowvera");
            p.setDescription(item.description());
            p.setIngredients(item.ingredients());
            p.setHowToUse(item.howToUse());
            p.setImageUrl("/images/" + item.slug() + ".jpg");   // files live in client/public/images
            p.setCategory(categories.findBySlug(item.category()).orElseThrow());
            p.setSkinTypes(new HashSet<>(item.skinTypes().stream()
                    .map(n -> skinTypes.findByName(n).orElseThrow()).toList()));
            p.setConcerns(new HashSet<>(item.concerns().stream()
                    .map(n -> concerns.findByName(n).orElseThrow()).toList()));
            products.save(p);

            for (Var v : item.variants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(p);
                variant.setSku(v.sku());
                variant.setName(v.name());
                variant.setPriceCents(v.rupees() * 100);
                variant.setLowStockThreshold(v.lowStock());
                // rule: variant stock always equals the sum of its batches
                variant.setStockQty(v.batches().stream().mapToInt(Batch::quantity).sum());
                variants.save(variant);

                for (Batch b : v.batches()) {
                    batches.save(new StockBatch(variant, b.code(), b.quantity(), today.plusDays(b.daysUntilExpiry())));
                }
            }
        }
        log.info("Seeded {} demo products", CATALOG.size());
    }
}
