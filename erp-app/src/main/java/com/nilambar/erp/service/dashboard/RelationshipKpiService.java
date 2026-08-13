package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.OrderFeedback;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.PurchaseOrder;
import com.nilambar.erp.domain.PurchaseOrderStatus;
import com.nilambar.erp.dto.dashboard.KpiCard;
import com.nilambar.erp.dto.dashboard.KpiGroup;
import com.nilambar.erp.dto.dashboard.KpiStatus;
import com.nilambar.erp.dto.dashboard.NamedValue;
import com.nilambar.erp.repository.OrderFeedbackRepository;
import com.nilambar.erp.repository.OrderRepository;
import com.nilambar.erp.repository.PurchaseOrderRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer satisfaction and vendor reliability KPIs. */
@Service
public class RelationshipKpiService {

    private final OrderRepository orderRepository;
    private final OrderFeedbackRepository feedbackRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final Clock clock;

    public RelationshipKpiService(OrderRepository orderRepository,
                                  OrderFeedbackRepository feedbackRepository,
                                  PurchaseOrderRepository purchaseOrderRepository,
                                  Clock clock) {
        this.orderRepository = orderRepository;
        this.feedbackRepository = feedbackRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RelationshipMetrics metrics(int windowDays) {
        Instant from = clock.instant().minus(Duration.ofDays(windowDays));
        List<CustomerOrder> orders = orderRepository.findByCreatedAtAfter(from);
        List<OrderFeedback> feedback = feedbackRepository.findAll();
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByOrderedAtAfter(from);

        Set<Long> customers = new HashSet<>();
        orders.forEach(order -> customers.add(order.getUser().getId()));

        double satisfaction = feedback.stream().mapToInt(OrderFeedback::getRating).average().orElse(0d);

        List<CustomerOrder> billable = orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();
        long fulfilled = billable.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED
                        || order.getStatus() == OrderStatus.PICKED_UP)
                .count();

        List<NamedValue> vendorScores = vendorScores(purchaseOrders);
        double vendorScore = vendorScores.stream().mapToDouble(NamedValue::value).average().orElse(0d);
        double avgResponse = purchaseOrders.stream()
                .map(PurchaseOrder::getVendor)
                .mapToInt(vendor -> vendor.getAvgResponseMinutes())
                .average()
                .orElse(0d);

        return new RelationshipMetrics(customers.size(),
                satisfaction,
                feedback.size(),
                vendorScore,
                KpiFormat.ratio(fulfilled, billable.size()),
                avgResponse,
                vendorScores);
    }

    public KpiGroup group(RelationshipMetrics m) {
        List<KpiCard> cards = List.of(
                new KpiCard("active-customers", "Active Customers", KpiFormat.count(m.activeCustomers()),
                        "ordered within the window", KpiStatus.NEUTRAL),
                new KpiCard("csat", "Customer Satisfaction Score", KpiFormat.number(m.satisfactionScore()) + " / 5",
                        "%d review(s)".formatted(m.feedbackCount()),
                        KpiFormat.higherIsBetter(m.satisfactionScore(), 4, 3)),
                new KpiCard("vendor-performance", "Vendor Performance Score",
                        KpiFormat.percent(m.vendorPerformanceScore()), "on-time purchase order delivery",
                        KpiFormat.higherIsBetter(m.vendorPerformanceScore(), 85, 70)),
                new KpiCard("fulfilment", "Order Fulfilment Rate", KpiFormat.percent(m.orderFulfilmentRate()),
                        "delivered or picked up", KpiFormat.higherIsBetter(m.orderFulfilmentRate(), 80, 60)),
                new KpiCard("response-time", "Average Response Time",
                        KpiFormat.number(m.averageResponseMinutes()) + " min", "vendor acknowledgement time",
                        KpiFormat.lowerIsBetter(m.averageResponseMinutes(), 120, 240)));
        return new KpiGroup("relationships", "Customers & Vendors", "\uD83E\uDD1D", cards);
    }

    private List<NamedValue> vendorScores(List<PurchaseOrder> purchaseOrders) {
        Map<String, int[]> tally = new LinkedHashMap<>();
        for (PurchaseOrder po : purchaseOrders) {
            if (po.getStatus() != PurchaseOrderStatus.RECEIVED || po.getReceivedAt() == null) {
                continue;
            }
            int[] counts = tally.computeIfAbsent(po.getVendor().getName(), key -> new int[2]);
            counts[1]++;
            if (!po.getReceivedAt().isAfter(po.getExpectedAt())) {
                counts[0]++;
            }
        }
        List<NamedValue> scores = new ArrayList<>();
        tally.forEach((vendor, counts) -> {
            double score = KpiFormat.ratio(counts[0], counts[1]);
            scores.add(new NamedValue(vendor, score, KpiFormat.percent(score)));
        });
        scores.sort(Comparator.comparingDouble(NamedValue::value).reversed());
        return scores;
    }
}
