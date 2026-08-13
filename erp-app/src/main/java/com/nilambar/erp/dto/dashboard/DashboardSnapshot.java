package com.nilambar.erp.dto.dashboard;

import java.time.Instant;
import java.util.List;

/** Everything the dashboard page (and its JSON twin) renders. */
public record DashboardSnapshot(Instant generatedAt,
                                int windowDays,
                                List<KpiGroup> groups,
                                List<TrendPoint> revenueTrend,
                                List<TrendPoint> revenueForecast,
                                List<NamedValue> topProducts,
                                List<NamedValue> teamProductivity,
                                List<ReorderSuggestion> reorderSuggestions,
                                List<AlertItem> alerts,
                                String aiNarrative) {

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.time.Instant getGeneratedAt() {
        return generatedAt;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public int getWindowDays() {
        return windowDays;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.util.List<KpiGroup> getGroups() {
        return groups;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.util.List<TrendPoint> getRevenueTrend() {
        return revenueTrend;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.util.List<TrendPoint> getRevenueForecast() {
        return revenueForecast;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.util.List<NamedValue> getTopProducts() {
        return topProducts;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.util.List<NamedValue> getTeamProductivity() {
        return teamProductivity;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.util.List<ReorderSuggestion> getReorderSuggestions() {
        return reorderSuggestions;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public java.util.List<AlertItem> getAlerts() {
        return alerts;
    }

    /** JSP EL accessor; Jakarta EL 5 does not resolve record components. */
    public String getAiNarrative() {
        return aiNarrative;
    }
}
