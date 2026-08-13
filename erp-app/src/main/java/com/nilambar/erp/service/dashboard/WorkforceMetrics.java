package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.NamedValue;
import com.nilambar.erp.dto.dashboard.TrendPoint;
import java.util.List;

public record WorkforceMetrics(int headcount,
                               int availableEmployees,
                               int allocatedEmployees,
                               int onLeaveEmployees,
                               double utilizationRate,
                               double overtimeHours,
                               String allocationStatus,
                               double attendanceRate,
                               double absenteeismRate,
                               double performanceScore,
                               double taskCompletionRate,
                               double trainingCompletionRate,
                               double retentionRate,
                               int pendingTasks,
                               int overdueTasks,
                               int shortageTeams,
                               List<NamedValue> productivityPerTeam,
                               List<TrendPoint> weeklyCompletedTasks) {
}
