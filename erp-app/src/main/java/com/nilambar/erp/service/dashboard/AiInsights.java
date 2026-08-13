package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.ReorderSuggestion;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import java.util.List;

public record AiInsights(double demandForecastUnits,
                         double revenuePrediction,
                         double workforceDemandFte,
                         int recommendedReorderQuantity,
                         double productivityTrendPercent,
                         double businessGrowthIndex,
                         List<TrendPoint> revenueForecast,
                         List<ReorderSuggestion> recommendedReorders,
                         String narrative) {
}
