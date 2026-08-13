package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.ReorderSuggestion;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import java.math.BigDecimal;
import java.util.List;

public record InventoryMetrics(long totalStockUnits,
                               int lowStockItems,
                               int outOfStockItems,
                               double turnoverRate,
                               double averageStockAgeDays,
                               int reorderAlerts,
                               BigDecimal inventoryValue,
                               double dailyDemandUnits,
                               List<ReorderSuggestion> reorderSuggestions,
                               List<TrendPoint> dailyUnitsSold) {
}
