package com.nilambar.erp.service.dashboard;

import com.nilambar.erp.dto.dashboard.KpiStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Presentation helpers shared by the KPI services. */
public final class KpiFormat {

    private KpiFormat() {
    }

    public static String currency(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount.setScale(0, RoundingMode.HALF_UP);
        return "\u20b9" + String.format(Locale.ENGLISH, "%,d", value.longValue());
    }

    public static String currency(double amount) {
        return currency(BigDecimal.valueOf(amount));
    }

    public static String percent(double value) {
        return String.format(Locale.ENGLISH, "%.1f%%", value);
    }

    public static String signedPercent(double value) {
        return String.format(Locale.ENGLISH, "%s%.1f%%", value >= 0 ? "+" : "", value);
    }

    public static String number(double value) {
        return String.format(Locale.ENGLISH, "%,.1f", value);
    }

    public static String count(long value) {
        return String.format(Locale.ENGLISH, "%,d", value);
    }

    public static double ratio(double numerator, double denominator) {
        return denominator == 0d ? 0d : numerator / denominator * 100d;
    }

    /** Status for metrics where a higher value is better. */
    public static KpiStatus higherIsBetter(double value, double warnBelow, double criticalBelow) {
        if (value < criticalBelow) {
            return KpiStatus.CRITICAL;
        }
        if (value < warnBelow) {
            return KpiStatus.WARN;
        }
        return KpiStatus.GOOD;
    }

    /** Status for metrics where a lower value is better. */
    public static KpiStatus lowerIsBetter(double value, double warnAbove, double criticalAbove) {
        if (value > criticalAbove) {
            return KpiStatus.CRITICAL;
        }
        if (value > warnAbove) {
            return KpiStatus.WARN;
        }
        return KpiStatus.GOOD;
    }
}
