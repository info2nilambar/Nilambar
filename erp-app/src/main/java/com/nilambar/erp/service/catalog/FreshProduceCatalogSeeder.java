package com.nilambar.erp.service.catalog;

import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.StockMovement;
import com.nilambar.erp.domain.StockMovementType;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds the fresh-produce range (10 vegetables and 10 fruits) to the catalog. Rows are matched by SKU
 * and only inserted when missing, so restarting the application never duplicates them and an
 * operator who edited a price keeps their change.
 *
 * <p>Runs before {@code DashboardDemoDataSeeder} so demo order history covers produce as well.
 */
@Component
@Order(10)
public class FreshProduceCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FreshProduceCatalogSeeder.class);

    private static final String VEGETABLES = "Vegetables";
    private static final String FRUITS = "Fruits";
    /** Produce is fast moving and perishable, so cost sits close to price and cover is short. */
    private static final BigDecimal COST_RATIO = new BigDecimal("0.72");

    /**
     * @param unit the sold-by unit shown in the product name, e.g. {@code 1 kg} or {@code 6 pcs}
     */
    private record ProduceItem(String sku,
                               String displayName,
                               String unit,
                               String category,
                               String note,
                               BigDecimal price,
                               int stockQuantity,
                               int reorderLevel) {

        String name() {
            return displayName + " " + unit;
        }

        String description() {
            return "%s (%s), %s Sourced from regional mandis, graded and cold-chained; ordered today, "
                    .formatted(displayName, unit, note)
                    + "delivered today inside the service radius.";
        }
    }

    private static final List<ProduceItem> PRODUCE = List.of(
            vegetable("VEG-001", "Tomato", "1 kg", "firm and ripe for daily cooking.", "38.00", 320, 80),
            vegetable("VEG-002", "Potato", "2 kg", "all-purpose table variety.", "64.00", 400, 90),
            vegetable("VEG-003", "Onion", "1 kg", "medium-sized, low moisture.", "42.00", 380, 90),
            vegetable("VEG-004", "Cauliflower", "1 pc", "tight white curd, leaves trimmed.", "45.00", 150, 40),
            vegetable("VEG-005", "Spinach", "500 g", "hand-picked tender bunches.", "28.00", 120, 45),
            vegetable("VEG-006", "Carrot", "1 kg", "sweet Ooty carrots.", "58.00", 210, 55),
            vegetable("VEG-007", "Brinjal", "500 g", "glossy purple, seedless grade.", "34.00", 160, 45),
            vegetable("VEG-008", "Lady Finger", "500 g", "snap-tender okra.", "36.00", 140, 40),
            vegetable("VEG-009", "Capsicum", "500 g", "thick-walled green peppers.", "52.00", 130, 40),
            vegetable("VEG-010", "Cucumber", "1 kg", "crisp salad cucumbers.", "40.00", 180, 50),
            fruit("FRT-001", "Banana", "1 dozen", "Yelakki, naturally ripened.", "62.00", 260, 70),
            fruit("FRT-002", "Apple", "1 kg", "Shimla, hand-graded.", "168.00", 190, 50),
            fruit("FRT-003", "Mango", "1 kg", "Alphonso, carbide free.", "220.00", 140, 45),
            fruit("FRT-004", "Orange", "1 kg", "Nagpur, high juice content.", "96.00", 200, 55),
            fruit("FRT-005", "Grapes", "500 g", "seedless Thompson.", "78.00", 170, 50),
            fruit("FRT-006", "Papaya", "1 pc", "semi-ripe, approx. 1.2 kg.", "72.00", 110, 35),
            fruit("FRT-007", "Pomegranate", "1 kg", "Bhagwa, deep red arils.", "195.00", 120, 40),
            fruit("FRT-008", "Watermelon", "1 pc", "Kiran, approx. 3 kg.", "88.00", 95, 30),
            fruit("FRT-009", "Pineapple", "1 pc", "Queen, crown trimmed.", "84.00", 105, 30),
            fruit("FRT-010", "Guava", "1 kg", "Allahabad Safeda.", "76.00", 130, 40));

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final Clock clock;

    public FreshProduceCatalogSeeder(ProductRepository productRepository,
                                     StockMovementRepository stockMovementRepository,
                                     Clock clock) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        List<Product> products = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();

        for (ProduceItem item : PRODUCE) {
            if (productRepository.findBySku(item.sku()).isPresent()) {
                continue;
            }
            Product product = toProduct(item, now);
            products.add(product);
            movements.add(openingStock(product, now));
        }
        if (products.isEmpty()) {
            return;
        }

        productRepository.saveAll(products);
        stockMovementRepository.saveAll(movements);
        log.info("Added {} fresh-produce products to the catalog", products.size());
    }

    private Product toProduct(ProduceItem item, Instant now) {
        Product product = new Product();
        product.setSku(item.sku());
        product.setName(item.name());
        product.setDescription(item.description());
        product.setCategory(item.category());
        product.setPrice(item.price());
        product.setUnitCost(item.price().multiply(COST_RATIO).setScale(2, RoundingMode.HALF_UP));
        product.setStockQuantity(item.stockQuantity());
        product.setReorderLevel(item.reorderLevel());
        product.setLastRestockedAt(now);
        product.setImagePath("/images/products/" + item.sku() + ".png");
        product.setActive(true);
        return product;
    }

    /** Opening balance in the ledger so inventory KPIs see where the stock came from. */
    private StockMovement openingStock(Product product, Instant now) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(StockMovementType.IN);
        movement.setQuantity(product.getStockQuantity());
        movement.setUnitCost(product.getUnitCost());
        movement.setOccurredAt(now);
        movement.setReference("CATALOG-" + product.getSku());
        return movement;
    }

    private static ProduceItem vegetable(String sku, String name, String unit, String note, String price,
                                         int stock, int reorderLevel) {
        return new ProduceItem(sku, name, unit, VEGETABLES, note, new BigDecimal(price), stock, reorderLevel);
    }

    private static ProduceItem fruit(String sku, String name, String unit, String note, String price,
                                     int stock, int reorderLevel) {
        return new ProduceItem(sku, name, unit, FRUITS, note, new BigDecimal(price), stock, reorderLevel);
    }
}
