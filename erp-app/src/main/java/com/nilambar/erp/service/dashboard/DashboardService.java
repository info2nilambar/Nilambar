package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.DashboardSnapshot;
import com.nilambar.erp.dto.dashboard.KpiGroup;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the full dashboard payload from the individual KPI services. */
@Service
public class DashboardService {

    public static final int DEFAULT_WINDOW_DAYS = 30;
    private static final int MIN_WINDOW_DAYS = 7;
    private static final int MAX_WINDOW_DAYS = 180;

    private final InventoryKpiService inventoryKpiService;
    private final WorkforceKpiService workforceKpiService;
    private final SalesKpiService salesKpiService;
    private final RelationshipKpiService relationshipKpiService;
    private final RiskKpiService riskKpiService;
    private final AiInsightService aiInsightService;
    private final Clock clock;

    public DashboardService(InventoryKpiService inventoryKpiService,
                            WorkforceKpiService workforceKpiService,
                            SalesKpiService salesKpiService,
                            RelationshipKpiService relationshipKpiService,
                            RiskKpiService riskKpiService,
                            AiInsightService aiInsightService,
                            Clock clock) {
        this.inventoryKpiService = inventoryKpiService;
        this.workforceKpiService = workforceKpiService;
        this.salesKpiService = salesKpiService;
        this.relationshipKpiService = relationshipKpiService;
        this.riskKpiService = riskKpiService;
        this.aiInsightService = aiInsightService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardSnapshot snapshot(int requestedWindowDays) {
        int windowDays = Math.max(MIN_WINDOW_DAYS, Math.min(MAX_WINDOW_DAYS, requestedWindowDays));

        InventoryMetrics inventory = inventoryKpiService.metrics(windowDays);
        WorkforceMetrics workforce = workforceKpiService.metrics(windowDays);
        SalesMetrics sales = salesKpiService.metrics(windowDays);
        RelationshipMetrics relationships = relationshipKpiService.metrics(windowDays);
        RiskMetrics risk = riskKpiService.metrics(inventory, workforce, sales, relationships);
        AiInsights ai = aiInsightService.insights(inventory, workforce, sales);

        List<KpiGroup> groups = List.of(
                inventoryKpiService.group(inventory),
                workforceKpiService.group(workforce),
                salesKpiService.group(sales),
                relationshipKpiService.group(relationships),
                riskKpiService.group(risk),
                aiInsightService.group(ai));

        return new DashboardSnapshot(clock.instant(),
                windowDays,
                groups,
                sales.monthlyRevenue(),
                ai.revenueForecast(),
                sales.topProducts(),
                workforce.productivityPerTeam(),
                ai.recommendedReorders(),
                risk.alerts(),
                ai.narrative());
    }
}
