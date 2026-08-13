package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.NamedValue;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import java.math.BigDecimal;
import java.util.List;

public record SalesMetrics(BigDecimal totalRevenue,
                           double monthlySalesGrowth,
                           double profitMargin,
                           double forecastAccuracy,
                           double customerAcquisitionRate,
                           double customerRetentionRate,
                           int orderCount,
                           List<NamedValue> topProducts,
                           List<TrendPoint> dailyRevenue,
                           List<TrendPoint> monthlyRevenue) {
}
