package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.domain.Project;
import com.nilambar.erp.domain.ProjectStatus;
import com.nilambar.erp.dto.dashboard.AlertItem;
import com.nilambar.erp.dto.dashboard.KpiCard;
import com.nilambar.erp.dto.dashboard.KpiGroup;
import com.nilambar.erp.dto.dashboard.KpiStatus;
import com.nilambar.erp.dto.dashboard.ReorderSuggestion;
import com.nilambar.erp.repository.ProjectRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-domain risk KPIs. Everything here is a composite of the other KPI services plus project
 * schedule data, so the inputs are passed in rather than re-queried.
 */
@Service
public class RiskKpiService {

    private static final int CRITICAL_COVER_DAYS = 5;

    private final ProjectRepository projectRepository;
    private final Clock clock;

    public RiskKpiService(ProjectRepository projectRepository, Clock clock) {
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RiskMetrics metrics(InventoryMetrics inventory, WorkforceMetrics workforce, SalesMetrics sales,
                               RelationshipMetrics relationships) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        int projectDelays = (int) projectRepository.findAll().stream().filter(p -> isDelayed(p, today)).count();

        int criticalStock = inventory.outOfStockItems() + (int) inventory.reorderSuggestions().stream()
                .filter(s -> s.stockOnHand() > 0 && s.daysOfCoverLeft() <= CRITICAL_COVER_DAYS)
                .count();

        double efficiency = Forecaster.clamp(0.3 * relationships.orderFulfilmentRate()
                + 0.25 * workforce.taskCompletionRate()
                + 0.2 * workforce.attendanceRate()
                + 0.15 * Math.min(100d, workforce.utilizationRate())
                + 0.1 * Forecaster.clamp(sales.forecastAccuracy()));

        double riskIndex = Forecaster.clamp((100d - efficiency)
                + criticalStock * 3d
                + projectDelays * 4d
                + workforce.shortageTeams() * 3d
                + Math.max(0d, 5d - relationships.satisfactionScore()) * 4d);

        return new RiskMetrics(criticalStock,
                projectDelays,
                workforce.shortageTeams(),
                workforce.pendingTasks(),
                efficiency,
                riskIndex,
                alerts(inventory, workforce, relationships, projectDelays, criticalStock));
    }

    public KpiGroup group(RiskMetrics m) {
        List<KpiCard> cards = List.of(
                new KpiCard("critical-stock", "Critical Stock Alerts", KpiFormat.count(m.criticalStockAlerts()),
                        "out of stock or <%d days cover".formatted(CRITICAL_COVER_DAYS),
                        KpiFormat.lowerIsBetter(m.criticalStockAlerts(), 1, 4)),
                new KpiCard("project-delays", "Project Delays", KpiFormat.count(m.projectDelays()),
                        "past planned end date", KpiFormat.lowerIsBetter(m.projectDelays(), 0, 2)),
                new KpiCard("workforce-shortage", "Workforce Shortages", KpiFormat.count(m.workforceShortages()),
                        "teams with nobody available", KpiFormat.lowerIsBetter(m.workforceShortages(), 0, 2)),
                new KpiCard("pending-tasks", "Pending Tasks", KpiFormat.count(m.pendingTasks()),
                        "not yet completed", KpiFormat.lowerIsBetter(m.pendingTasks(), 15, 30)),
                new KpiCard("efficiency", "Operational Efficiency Score", KpiFormat.percent(m.operationalEfficiency()),
                        "fulfilment, tasks, attendance blend",
                        KpiFormat.higherIsBetter(m.operationalEfficiency(), 75, 60)),
                new KpiCard("risk-index", "Business Risk Index", KpiFormat.number(m.businessRiskIndex()) + " / 100",
                        "higher means more exposure",
                        KpiFormat.lowerIsBetter(m.businessRiskIndex(), 35, 60)));
        return new KpiGroup("risk", "Risk & Operations", "\uD83D\uDEA8", cards);
    }

    private boolean isDelayed(Project project, LocalDate today) {
        if (project.getStatus() == ProjectStatus.COMPLETED) {
            return project.getActualEndOn() != null && project.getActualEndOn().isAfter(project.getPlannedEndOn());
        }
        return project.getPlannedEndOn().isBefore(today);
    }

    private List<AlertItem> alerts(InventoryMetrics inventory, WorkforceMetrics workforce,
                                   RelationshipMetrics relationships, int projectDelays, int criticalStock) {
        List<AlertItem> alerts = new ArrayList<>();
        for (ReorderSuggestion suggestion : inventory.reorderSuggestions().stream().limit(3).toList()) {
            alerts.add(new AlertItem("Inventory",
                    "%s is down to %d unit(s); reorder %d".formatted(suggestion.productName(),
                            suggestion.stockOnHand(), suggestion.recommendedQuantity()),
                    suggestion.stockOnHand() == 0 ? KpiStatus.CRITICAL : KpiStatus.WARN));
        }
        if (criticalStock == 0) {
            alerts.add(new AlertItem("Inventory", "No critical stock exposure right now", KpiStatus.GOOD));
        }
        if (projectDelays > 0) {
            alerts.add(new AlertItem("Projects", "%d project(s) are past their planned end date"
                    .formatted(projectDelays), KpiStatus.WARN));
        }
        if (workforce.shortageTeams() > 0) {
            alerts.add(new AlertItem("Workforce", "%d team(s) have nobody available for new work"
                    .formatted(workforce.shortageTeams()), KpiStatus.WARN));
        }
        if (workforce.overdueTasks() > 0) {
            alerts.add(new AlertItem("Workforce", "%d task(s) are overdue".formatted(workforce.overdueTasks()),
                    KpiStatus.WARN));
        }
        if (relationships.orderFulfilmentRate() < 70d) {
            alerts.add(new AlertItem("Operations", "Order fulfilment is at %s"
                    .formatted(KpiFormat.percent(relationships.orderFulfilmentRate())), KpiStatus.CRITICAL));
        }
        return alerts;
    }
}
