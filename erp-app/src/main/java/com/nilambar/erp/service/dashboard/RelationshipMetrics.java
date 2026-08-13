package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.NamedValue;
import java.util.List;

/** Customer- and vendor-facing metrics. */
public record RelationshipMetrics(int activeCustomers,
                                  double satisfactionScore,
                                  int feedbackCount,
                                  double vendorPerformanceScore,
                                  double orderFulfilmentRate,
                                  double averageResponseMinutes,
                                  List<NamedValue> vendorScores) {
}
