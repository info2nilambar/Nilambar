package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.StockMovement;
import com.nilambar.erp.domain.StockMovementType;
import com.nilambar.erp.dto.dashboard.KpiCard;
import com.nilambar.erp.dto.dashboard.KpiGroup;
import com.nilambar.erp.dto.dashboard.KpiStatus;
import com.nilambar.erp.dto.dashboard.ReorderSuggestion;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inventory KPIs derived from the product catalogue and its stock movement ledger. */
@Service
public class InventoryKpiService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");
    private static final int LEAD_TIME_DAYS = 5;
    private static final int SAFETY_DAYS = 7;

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final Clock clock;

    public InventoryKpiService(ProductRepository productRepository,
                               StockMovementRepository stockMovementRepository,
                               Clock clock) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public InventoryMetrics metrics(int windowDays) {
        Instant now = clock.instant();
        Instant from = now.minus(Duration.ofDays(windowDays));
        List<Product> products = productRepository.findAll().stream().filter(Product::isActive).toList();
        List<StockMovement> movements = stockMovementRepository.findByOccurredAtAfter(from);

        long totalStock = products.stream().mapToLong(Product::getStockQuantity).sum();
        int lowStock = (int) products.stream().filter(Product::isLowStock).count();
        int outOfStock = (int) products.stream().filter(p -> p.getStockQuantity() <= 0).count();
        BigDecimal inventoryValue = products.stream()
                .map(p -> p.getUnitCost().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long unitsOut = movements.stream()
                .filter(m -> m.getMovementType() == StockMovementType.OUT)
                .mapToLong(StockMovement::getQuantity)
                .sum();
        double averageStock = totalStock + unitsOut / 2d;
        double turnover = averageStock == 0d ? 0d : (unitsOut / averageStock) * (365d / windowDays);
        double dailyDemand = unitsOut / (double) windowDays;

        return new InventoryMetrics(totalStock,
                lowStock,
                outOfStock,
                turnover,
                averageStockAgeDays(products, now),
                lowStock + outOfStock,
                inventoryValue,
                dailyDemand,
                reorderSuggestions(products, movements, windowDays),
                dailyUnitsSold(movements, windowDays, now));
    }

    public KpiGroup group(InventoryMetrics m) {
        List<KpiCard> cards = List.of(
                new KpiCard("total-stock", "Total Stock Available", KpiFormat.count(m.totalStockUnits()),
                        "units across active SKUs", KpiStatus.NEUTRAL),
                new KpiCard("low-stock", "Low Stock Items", KpiFormat.count(m.lowStockItems()),
                        "at or below reorder level", KpiFormat.lowerIsBetter(m.lowStockItems(), 2, 5)),
                new KpiCard("out-of-stock", "Out-of-Stock Items", KpiFormat.count(m.outOfStockItems()),
                        "SKUs unavailable to sell", KpiFormat.lowerIsBetter(m.outOfStockItems(), 0, 2)),
                new KpiCard("turnover", "Inventory Turnover Rate", KpiFormat.number(m.turnoverRate()) + "x",
                        "annualised from stock movements", KpiFormat.higherIsBetter(m.turnoverRate(), 4, 2)),
                new KpiCard("stock-aging", "Stock Aging", KpiFormat.number(m.averageStockAgeDays()) + " days",
                        "average days since last restock", KpiFormat.lowerIsBetter(m.averageStockAgeDays(), 45, 90)),
                new KpiCard("reorder-alerts", "Reorder Alerts", KpiFormat.count(m.reorderAlerts()),
                        "SKUs needing a purchase order", KpiFormat.lowerIsBetter(m.reorderAlerts(), 2, 5)),
                new KpiCard("inventory-value", "Inventory Value", KpiFormat.currency(m.inventoryValue()),
                        "stock on hand at cost", KpiStatus.NEUTRAL));
        return new KpiGroup("inventory", "Inventory", "\uD83D\uDCE6", cards);
    }

    private double averageStockAgeDays(List<Product> products, Instant now) {
        long weightedDays = 0;
        long units = 0;
        for (Product product : products) {
            if (product.getStockQuantity() <= 0 || product.getLastRestockedAt() == null) {
                continue;
            }
            long age = Duration.between(product.getLastRestockedAt(), now).toDays();
            weightedDays += age * product.getStockQuantity();
            units += product.getStockQuantity();
        }
        return units == 0 ? 0d : (double) weightedDays / units;
    }

    private List<ReorderSuggestion> reorderSuggestions(List<Product> products,
                                                       List<StockMovement> movements,
                                                       int windowDays) {
        Map<Long, Long> soldPerProduct = new LinkedHashMap<>();
        for (StockMovement movement : movements) {
            if (movement.getMovementType() == StockMovementType.OUT) {
                soldPerProduct.merge(movement.getProduct().getId(), (long) movement.getQuantity(), Long::sum);
            }
        }
        List<ReorderSuggestion> suggestions = new ArrayList<>();
        for (Product product : products) {
            double dailyDemand = soldPerProduct.getOrDefault(product.getId(), 0L) / (double) windowDays;
            boolean needsReorder = product.getStockQuantity() <= product.getReorderLevel();
            if (!needsReorder) {
                continue;
            }
            int quantity = Forecaster.recommendedReorderQuantity(dailyDemand, LEAD_TIME_DAYS, SAFETY_DAYS,
                    product.getStockQuantity());
            int daysOfCover = dailyDemand <= 0d ? Integer.MAX_VALUE
                    : (int) Math.floor(product.getStockQuantity() / dailyDemand);
            suggestions.add(new ReorderSuggestion(product.getSku(), product.getName(), product.getStockQuantity(),
                    product.getReorderLevel(), dailyDemand, Math.max(quantity, product.getReorderLevel()),
                    Math.min(daysOfCover, 999)));
        }
        suggestions.sort(Comparator.comparingInt(ReorderSuggestion::daysOfCoverLeft));
        return suggestions;
    }

    private List<TrendPoint> dailyUnitsSold(List<StockMovement> movements, int windowDays, Instant now) {
        Map<LocalDate, Double> perDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        for (int i = windowDays - 1; i >= 0; i--) {
            perDay.put(today.minusDays(i), 0d);
        }
        for (StockMovement movement : movements) {
            if (movement.getMovementType() != StockMovementType.OUT) {
                continue;
            }
            LocalDate day = LocalDate.ofInstant(movement.getOccurredAt(), ZoneOffset.UTC);
            perDay.computeIfPresent(day, (key, value) -> value + movement.getQuantity());
        }
        return perDay.entrySet().stream()
                .map(e -> new TrendPoint(e.getKey().format(DAY), e.getValue()))
                .toList();
    }
}
