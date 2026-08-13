package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.OrderItem;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.dto.dashboard.KpiCard;
import com.nilambar.erp.dto.dashboard.KpiGroup;
import com.nilambar.erp.dto.dashboard.KpiStatus;
import com.nilambar.erp.dto.dashboard.NamedValue;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import com.nilambar.erp.repository.OrderRepository;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Revenue, margin and customer-growth KPIs derived from placed orders. */
@Service
public class SalesKpiService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yy");
    private static final int HISTORY_DAYS = 210;
    private static final int MONTHS_SHOWN = 6;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public SalesKpiService(OrderRepository orderRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           Clock clock) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SalesMetrics metrics(int windowDays) {
        Instant now = clock.instant();
        Instant historyFrom = now.minus(Duration.ofDays(HISTORY_DAYS));
        Instant windowFrom = now.minus(Duration.ofDays(windowDays));
        Instant previousFrom = now.minus(Duration.ofDays(2L * windowDays));

        List<CustomerOrder> history = orderRepository.findByCreatedAtAfter(historyFrom).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .toList();
        List<CustomerOrder> inWindow = history.stream()
                .filter(o -> o.getCreatedAt().isAfter(windowFrom))
                .toList();
        List<CustomerOrder> previousWindow = history.stream()
                .filter(o -> o.getCreatedAt().isAfter(previousFrom) && !o.getCreatedAt().isAfter(windowFrom))
                .toList();

        BigDecimal revenue = sum(inWindow);
        BigDecimal previousRevenue = sum(previousWindow);
        double growth = previousRevenue.signum() == 0
                ? (revenue.signum() == 0 ? 0d : 100d)
                : revenue.subtract(previousRevenue).doubleValue() / previousRevenue.doubleValue() * 100d;

        List<TrendPoint> daily = dailyRevenue(history, windowDays, now);
        double accuracy = Forecaster.backTestAccuracy(daily.stream().map(TrendPoint::value).toList(),
                Math.max(3, windowDays / 3));

        return new SalesMetrics(revenue,
                growth,
                profitMargin(inWindow),
                accuracy,
                acquisitionRate(windowFrom),
                retentionRate(history, windowFrom),
                inWindow.size(),
                topProducts(inWindow),
                daily,
                monthlyRevenue(history, now));
    }

    public KpiGroup group(SalesMetrics m) {
        List<KpiCard> cards = List.of(
                new KpiCard("revenue", "Total Revenue", KpiFormat.currency(m.totalRevenue()),
                        "%d order(s) in the window".formatted(m.orderCount()), KpiStatus.NEUTRAL),
                new KpiCard("sales-growth", "Monthly Sales Growth", KpiFormat.signedPercent(m.monthlySalesGrowth()),
                        "vs the preceding window",
                        m.monthlySalesGrowth() >= 0 ? KpiStatus.GOOD : KpiStatus.WARN),
                new KpiCard("profit-margin", "Profit Margin", KpiFormat.percent(m.profitMargin()),
                        "revenue less product cost", KpiFormat.higherIsBetter(m.profitMargin(), 25, 15)),
                new KpiCard("forecast-accuracy", "Sales Forecast Accuracy", KpiFormat.percent(m.forecastAccuracy()),
                        "back-test of the revenue model",
                        KpiFormat.higherIsBetter(m.forecastAccuracy(), 80, 60)),
                new KpiCard("acquisition", "Customer Acquisition Rate", KpiFormat.percent(m.customerAcquisitionRate()),
                        "new customers vs total base", KpiStatus.NEUTRAL),
                new KpiCard("retention", "Customer Retention Rate", KpiFormat.percent(m.customerRetentionRate()),
                        "returning buyers in the window",
                        KpiFormat.higherIsBetter(m.customerRetentionRate(), 40, 25)),
                new KpiCard("top-product", "Top-Selling Product",
                        m.topProducts().isEmpty() ? "n/a" : m.topProducts().get(0).name(),
                        m.topProducts().isEmpty() ? "no sales yet" : m.topProducts().get(0).display(),
                        KpiStatus.NEUTRAL));
        return new KpiGroup("sales", "Sales & Business", "\uD83D\uDCB0", cards);
    }

    private BigDecimal sum(List<CustomerOrder> orders) {
        return orders.stream().map(CustomerOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double profitMargin(List<CustomerOrder> orders) {
        Map<Long, BigDecimal> costBySku = new HashMap<>();
        for (Product product : productRepository.findAll()) {
            costBySku.put(product.getId(), product.getUnitCost());
        }
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (CustomerOrder order : orders) {
            for (OrderItem item : order.getItems()) {
                revenue = revenue.add(item.getLineTotal());
                BigDecimal unitCost = costBySku.getOrDefault(item.getProduct().getId(), BigDecimal.ZERO);
                cost = cost.add(unitCost.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        return revenue.signum() == 0 ? 0d
                : revenue.subtract(cost).doubleValue() / revenue.doubleValue() * 100d;
    }

    private double acquisitionRate(Instant windowFrom) {
        List<User> users = userRepository.findAll();
        long joined = users.stream().filter(u -> u.getCreatedAt().isAfter(windowFrom)).count();
        return KpiFormat.ratio(joined, users.size());
    }

    private double retentionRate(List<CustomerOrder> history, Instant windowFrom) {
        Set<Long> before = new HashSet<>();
        Set<Long> during = new HashSet<>();
        for (CustomerOrder order : history) {
            Long userId = order.getUser().getId();
            if (order.getCreatedAt().isAfter(windowFrom)) {
                during.add(userId);
            } else {
                before.add(userId);
            }
        }
        if (before.isEmpty()) {
            return 0d;
        }
        long returning = before.stream().filter(during::contains).count();
        return KpiFormat.ratio(returning, before.size());
    }

    private List<NamedValue> topProducts(List<CustomerOrder> orders) {
        Map<String, BigDecimal> revenuePerProduct = new LinkedHashMap<>();
        for (CustomerOrder order : orders) {
            for (OrderItem item : order.getItems()) {
                revenuePerProduct.merge(item.getProductName(), item.getLineTotal(), BigDecimal::add);
            }
        }
        return revenuePerProduct.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> new NamedValue(e.getKey(), e.getValue().doubleValue(), KpiFormat.currency(e.getValue())))
                .toList();
    }

    private List<TrendPoint> dailyRevenue(List<CustomerOrder> history, int windowDays, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Map<LocalDate, Double> perDay = new LinkedHashMap<>();
        for (int i = windowDays - 1; i >= 0; i--) {
            perDay.put(today.minusDays(i), 0d);
        }
        for (CustomerOrder order : history) {
            LocalDate day = LocalDate.ofInstant(order.getCreatedAt(), ZoneOffset.UTC);
            perDay.computeIfPresent(day, (key, value) -> value + order.getTotal().doubleValue());
        }
        return perDay.entrySet().stream().map(e -> new TrendPoint(e.getKey().format(DAY), e.getValue())).toList();
    }

    private List<TrendPoint> monthlyRevenue(List<CustomerOrder> history, Instant now) {
        YearMonth current = YearMonth.from(LocalDate.ofInstant(now, ZoneOffset.UTC));
        Map<YearMonth, Double> perMonth = new LinkedHashMap<>();
        for (int i = MONTHS_SHOWN - 1; i >= 0; i--) {
            perMonth.put(current.minusMonths(i), 0d);
        }
        for (CustomerOrder order : history) {
            YearMonth month = YearMonth.from(LocalDate.ofInstant(order.getCreatedAt(), ZoneOffset.UTC));
            perMonth.computeIfPresent(month, (key, value) -> value + order.getTotal().doubleValue());
        }
        List<TrendPoint> points = new ArrayList<>();
        perMonth.forEach((month, value) -> points.add(new TrendPoint(month.atDay(1).format(MONTH), value)));
        return points;
    }
}
