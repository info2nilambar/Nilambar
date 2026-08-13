package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.KpiCard;
import com.nilambar.erp.dto.dashboard.KpiGroup;
import com.nilambar.erp.dto.dashboard.KpiStatus;
import com.nilambar.erp.dto.dashboard.ReorderSuggestion;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Predictive KPIs. The models are deliberately lightweight (trend + exponential smoothing over the
 * observed series) so the numbers stay explainable and need no external inference service.
 */
@Service
public class AiInsightService {

    private static final int DEMAND_HORIZON_DAYS = 7;
    private static final int REVENUE_HORIZON_DAYS = 30;
    private static final double HOURS_PER_UNIT = 0.35;
    private static final double MONTHLY_HOURS_PER_FTE = 160d;

    public AiInsights insights(InventoryMetrics inventory, WorkforceMetrics workforce, SalesMetrics sales) {
        List<Double> demandSeries = inventory.dailyUnitsSold().stream().map(TrendPoint::value).toList();
        List<Double> revenueSeries = sales.dailyRevenue().stream().map(TrendPoint::value).toList();
        List<Double> productivitySeries = workforce.weeklyCompletedTasks().stream().map(TrendPoint::value).toList();

        double demandForecast = Forecaster.forecastTotal(demandSeries, DEMAND_HORIZON_DAYS);
        List<Double> revenueProjection = Forecaster.forecast(revenueSeries, REVENUE_HORIZON_DAYS);
        double revenuePrediction = revenueProjection.stream().mapToDouble(Double::doubleValue).sum();

        double monthlyDemandUnits = Forecaster.forecastTotal(demandSeries, 30);
        double workforceDemandFte = monthlyDemandUnits * HOURS_PER_UNIT / MONTHLY_HOURS_PER_FTE;

        int recommendedReorder = inventory.reorderSuggestions().stream()
                .mapToInt(ReorderSuggestion::recommendedQuantity)
                .sum();
        double productivityTrend = Forecaster.trendPercent(productivitySeries);

        double growthIndex = Forecaster.clamp(50d
                + 0.4 * clampChange(Forecaster.trendPercent(revenueSeries))
                + 0.2 * clampChange(Forecaster.trendPercent(demandSeries))
                + 0.2 * clampChange(productivityTrend)
                + 0.2 * (sales.customerRetentionRate() - 40d));

        return new AiInsights(demandForecast,
                revenuePrediction,
                workforceDemandFte,
                recommendedReorder,
                productivityTrend,
                growthIndex,
                forecastPoints(revenueProjection),
                inventory.reorderSuggestions().stream().limit(5).toList(),
                narrative(demandForecast, revenuePrediction, workforceDemandFte, workforce, productivityTrend));
    }

    public KpiGroup group(AiInsights ai) {
        List<KpiCard> cards = List.of(
                new KpiCard("demand-forecast", "Demand Forecast", KpiFormat.number(ai.demandForecastUnits()) + " units",
                        "next %d days".formatted(DEMAND_HORIZON_DAYS), KpiStatus.NEUTRAL),
                new KpiCard("revenue-prediction", "Revenue Prediction", KpiFormat.currency(ai.revenuePrediction()),
                        "next %d days".formatted(REVENUE_HORIZON_DAYS), KpiStatus.NEUTRAL),
                new KpiCard("workforce-forecast", "Workforce Demand Forecast",
                        KpiFormat.number(ai.workforceDemandFte()) + " FTE", "needed to serve forecast demand",
                        KpiStatus.NEUTRAL),
                new KpiCard("reorder-quantity", "Recommended Reorder Quantity",
                        KpiFormat.count(ai.recommendedReorderQuantity()) + " units",
                        "%d SKU(s) below reorder level".formatted(ai.recommendedReorders().size()),
                        ai.recommendedReorderQuantity() > 0 ? KpiStatus.WARN : KpiStatus.GOOD),
                new KpiCard("productivity-trend", "Productivity Trend",
                        KpiFormat.signedPercent(ai.productivityTrendPercent()), "recent vs earlier weeks",
                        ai.productivityTrendPercent() >= 0 ? KpiStatus.GOOD : KpiStatus.WARN),
                new KpiCard("growth-index", "Business Growth Index",
                        KpiFormat.number(ai.businessGrowthIndex()) + " / 100", "blended momentum score",
                        KpiFormat.higherIsBetter(ai.businessGrowthIndex(), 55, 40)));
        return new KpiGroup("ai", "AI-Driven Insights", "\uD83E\uDD16", cards);
    }

    private double clampChange(double percentChange) {
        return Math.max(-50d, Math.min(50d, percentChange));
    }

    private List<TrendPoint> forecastPoints(List<Double> projection) {
        List<TrendPoint> points = new ArrayList<>(projection.size());
        for (int i = 0; i < projection.size(); i++) {
            points.add(new TrendPoint("D+" + (i + 1), projection.get(i)));
        }
        return points;
    }

    private String narrative(double demandForecast, double revenuePrediction, double workforceDemandFte,
                             WorkforceMetrics workforce, double productivityTrend) {
        String staffing;
        double gap = workforceDemandFte - workforce.headcount();
        if (gap > 0.5) {
            staffing = "hire or reassign about %s more FTE".formatted(KpiFormat.number(gap));
        } else if (gap < -0.5) {
            staffing = "about %s FTE of spare capacity is available".formatted(KpiFormat.number(-gap));
        } else {
            staffing = "current staffing matches the forecast";
        }
        return "Demand for the next %d days is projected at %s units (%s over the next %d days). Productivity is %s; %s."
                .formatted(DEMAND_HORIZON_DAYS, KpiFormat.number(demandForecast),
                        KpiFormat.currency(revenuePrediction), REVENUE_HORIZON_DAYS,
                        KpiFormat.signedPercent(productivityTrend), staffing);
    }
}
