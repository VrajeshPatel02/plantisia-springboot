package com.nikyfarm.config;

import com.nikyfarm.entity.Category;
import com.nikyfarm.entity.Product;
import com.nikyfarm.entity.ProductVariant;
import com.nikyfarm.repository.CategoryRepository;
import com.nikyfarm.repository.ProductRepository;
import com.nikyfarm.repository.ProductVariantRepository;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevCatalogSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public void run(String... args) {

        /*
         * Do not seed again if products already exist.
         */
        if (productRepository.count() > 0) {
            return;
        }

        // ============================================================
        // CATEGORIES
        // ============================================================

        Category plants = createCategory(
                "Plants",
                "plants",
                "Indoor, outdoor and decorative plants for every space.",
                1
        );

        Category pots = createCategory(
                "Pots",
                "pots",
                "Decorative and functional pots and planters for your plants.",
                2
        );

        Category fertilisers = createCategory(
                "Fertilisers",
                "fertilisers",
                "Plant nutrients, fertilisers and natural plant care products.",
                3
        );

        Category seeds = createCategory(
                "Seeds",
                "seeds",
                "Quality seeds for growing flowers, vegetables and herbs.",
                4
        );

        Category gardenTools = createCategory(
                "Garden Tools",
                "garden-tools",
                "Essential tools for planting, pruning and maintaining your garden.",
                5
        );

        // ============================================================
        // PLANTS
        // ============================================================

        createProduct(
                plants,
                "Monstera Deliciosa",
                "monstera-deliciosa",
                "A beautiful tropical indoor plant with iconic split leaves.",
                "Monstera Deliciosa is a popular tropical houseplant known for its large, naturally split leaves. It adds a bold green look to living rooms, offices and indoor spaces.",
                699.0,
                599.0,
                "Medium",
                "Green",
                599.0,
                499.0,
                25,
                "NIKY-MONSTERA-M"
        );

        createProduct(
                plants,
                "Snake Plant",
                "snake-plant",
                "A hardy low-maintenance plant perfect for beginners.",
                "Snake Plant is one of the easiest indoor plants to maintain. It tolerates low light and requires minimal watering, making it suitable for homes and offices.",
                449.0,
                349.0,
                "Medium",
                "Green",
                349.0,
                299.0,
                35,
                "NIKY-SNAKE-M"
        );

        createProduct(
                plants,
                "Peace Lily",
                "peace-lily",
                "An elegant flowering indoor plant with lush green foliage.",
                "Peace Lily is valued for its attractive foliage and white flowers. It works well in bright indirect light and brings a fresh natural appearance indoors.",
                599.0,
                499.0,
                "Medium",
                "Green",
                499.0,
                399.0,
                20,
                "NIKY-PEACE-M"
        );

        createProduct(
                plants,
                "Areca Palm",
                "areca-palm",
                "A graceful tropical palm that brings a lush look indoors.",
                "Areca Palm is a popular decorative palm with soft arching fronds. It is ideal for living rooms, entrances and larger indoor spaces.",
                799.0,
                699.0,
                "Medium",
                "Green",
                699.0,
                599.0,
                18,
                "NIKY-ARECA-M"
        );

        // ============================================================
        // POTS
        // ============================================================

        createProduct(
                pots,
                "Terracotta Pot",
                "terracotta-pot",
                "Classic natural terracotta pot for everyday gardening.",
                "A simple and breathable terracotta pot suitable for a wide variety of indoor and outdoor plants.",
                249.0,
                199.0,
                "8 inch",
                "Terracotta",
                199.0,
                169.0,
                40,
                "NIKY-TERRA-8"
        );

        createProduct(
                pots,
                "Ceramic Planter",
                "ceramic-planter",
                "Modern ceramic planter designed for stylish indoor spaces.",
                "A clean and elegant ceramic planter that works beautifully with indoor plants and contemporary home interiors.",
                699.0,
                599.0,
                "8 inch",
                "White",
                599.0,
                499.0,
                20,
                "NIKY-CERAMIC-8"
        );

        createProduct(
                pots,
                "Self Watering Pot",
                "self-watering-pot",
                "Convenient planter with a built-in water reservoir.",
                "A practical self-watering planter designed to reduce watering frequency and provide consistent moisture to plants.",
                549.0,
                449.0,
                "8 inch",
                "White",
                449.0,
                399.0,
                15,
                "NIKY-SELF-8"
        );

        // ============================================================
        // FERTILISERS
        // ============================================================

        createProduct(
                fertilisers,
                "Organic Plant Food",
                "organic-plant-food",
                "Balanced organic nutrition for healthy plant growth.",
                "Organic Plant Food provides essential nutrients to support healthy foliage, stronger roots and overall plant development.",
                349.0,
                299.0,
                "500 g",
                "Natural",
                299.0,
                249.0,
                50,
                "NIKY-FOOD-500"
        );

        createProduct(
                fertilisers,
                "Vermicompost",
                "vermicompost",
                "Natural organic compost made from nutrient-rich worm castings.",
                "Vermicompost improves soil structure and provides natural nutrients for plants. Suitable for pots, gardens and vegetable plants.",
                299.0,
                249.0,
                "1 kg",
                "Natural",
                249.0,
                199.0,
                60,
                "NIKY-VERMI-1KG"
        );

        createProduct(
                fertilisers,
                "Seaweed Plant Tonic",
                "seaweed-plant-tonic",
                "Natural seaweed-based tonic for stronger plant growth.",
                "Seaweed Plant Tonic is a concentrated plant supplement designed to support healthy roots, foliage and overall plant development.",
                449.0,
                399.0,
                "500 ml",
                "Liquid",
                399.0,
                349.0,
                30,
                "NIKY-SEAWEED-500"
        );

        // ============================================================
        // SEEDS
        // ============================================================

        createProduct(
                seeds,
                "Basil Seeds",
                "basil-seeds",
                "Fresh basil seeds for growing herbs at home.",
                "Grow fresh basil at home using these quality seeds. Suitable for containers, balconies, kitchen gardens and outdoor spaces.",
                99.0,
                79.0,
                "Packet",
                "Natural",
                79.0,
                69.0,
                100,
                "NIKY-BASIL-PKT"
        );

        createProduct(
                seeds,
                "Marigold Seeds",
                "marigold-seeds",
                "Bright flowering marigold seeds for home gardens.",
                "Grow vibrant marigold flowers in containers or garden beds. These seeds are suitable for home gardeners and beginners.",
                99.0,
                79.0,
                "Packet",
                "Natural",
                79.0,
                69.0,
                100,
                "NIKY-MARIGOLD-PKT"
        );

        createProduct(
                seeds,
                "Tomato Seeds",
                "tomato-seeds",
                "Quality tomato seeds for growing fresh tomatoes at home.",
                "Grow fresh tomatoes in containers, balconies or home vegetable gardens with these easy-to-grow tomato seeds.",
                129.0,
                99.0,
                "Packet",
                "Natural",
                99.0,
                79.0,
                100,
                "NIKY-TOMATO-PKT"
        );

        // ============================================================
        // GARDEN TOOLS
        // ============================================================

        createProduct(
                gardenTools,
                "Hand Trowel",
                "hand-trowel",
                "Compact gardening trowel for planting and soil work.",
                "A practical hand trowel suitable for planting seedlings, transferring soil and maintaining potted plants.",
                249.0,
                199.0,
                "Standard",
                "Green",
                199.0,
                169.0,
                35,
                "NIKY-TROWEL-STD"
        );

        createProduct(
                gardenTools,
                "Pruning Shears",
                "pruning-shears",
                "Sharp and durable shears for pruning plants and small branches.",
                "Pruning Shears make it easy to trim stems, remove damaged growth and maintain the shape of plants.",
                499.0,
                399.0,
                "Standard",
                "Green",
                399.0,
                349.0,
                25,
                "NIKY-SHEARS-STD"
        );

        createProduct(
                gardenTools,
                "Watering Can",
                "watering-can",
                "Lightweight watering can for indoor and outdoor plants.",
                "A practical watering can designed for controlled watering of potted plants, balcony gardens and small outdoor spaces.",
                449.0,
                349.0,
                "5 Litre",
                "Green",
                349.0,
                299.0,
                20,
                "NIKY-WATER-5L"
        );
    }

    // ================================================================
    // CATEGORY CREATION
    // ================================================================

    private Category createCategory(
            String name,
            String slug,
            String description,
            int sortOrder) {

        Category category = new Category();

        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setSortOrder(sortOrder);
        category.setIsActive(true);

        category.setImageUrl(null);
        category.setImagePublicId(null);

        return categoryRepository.save(category);
    }

    // ================================================================
    // PRODUCT + VARIANT CREATION
    // ================================================================

    private void createProduct(
            Category category,
            String name,
            String slug,
            String shortDescription,
            String description,
            Double basePrice,
            Double discountedPrice,
            String variantSize,
            String variantColour,
            Double variantPrice,
            Double variantDiscountedPrice,
            Integer stock,
            String sku) {

        // ------------------------------------------------------------
        // Product
        // ------------------------------------------------------------

        Product product = new Product();

        product.setName(name);
        product.setSlug(slug);
        product.setShortDescription(shortDescription);
        product.setDescription(description);

        // Product uses BigDecimal for money
        product.setBasePrice(BigDecimal.valueOf(basePrice));
        product.setDiscountedPrice(BigDecimal.valueOf(discountedPrice));

        product.setAvgRating(0.0f);
        product.setReviewCount(0);
        product.setIsActive(true);
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        // ------------------------------------------------------------
        // Product Variant
        // ------------------------------------------------------------

        ProductVariant variant = new ProductVariant();

        variant.setProduct(savedProduct);
        variant.setVariantLabel(variantSize);
        variant.setSize(variantSize);
        variant.setColour(variantColour);

        // ProductVariant also uses BigDecimal for money
        variant.setPrice(BigDecimal.valueOf(variantPrice));
        variant.setDiscountedPrice(
                BigDecimal.valueOf(variantDiscountedPrice)
        );

        variant.setStockQuantity(stock);
        variant.setSku(sku);
        variant.setIsDefault(true);
        variant.setIsActive(true);

        productVariantRepository.save(variant);
    }
}