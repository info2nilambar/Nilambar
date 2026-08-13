package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.AlertItem;
import java.util.List;

public record RiskMetrics(int criticalStockAlerts,
                          int projectDelays,
                          int workforceShortages,
                          int pendingTasks,
                          double operationalEfficiency,
                          double businessRiskIndex,
                          List<AlertItem> alerts) {
}
