package com.nilambar.erp.service.dashboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Small forecasting toolkit backing the AI-driven KPIs. Ordinary least squares over the observation
 * index is combined with an exponentially weighted level so that a recent shift in the series moves
 * the forecast faster than a plain regression would.
 */
public final class Forecaster {

    private static final double SMOOTHING = 0.35;

    private Forecaster() {
    }

    /** Slope of the least-squares line through {@code series} (units per step). */
    public static double slope(List<Double> series) {
        int n = series.size();
        if (n < 2) {
            return 0d;
        }
        double meanX = (n - 1) / 2d;
        double meanY = mean(series);
        double covariance = 0d;
        double variance = 0d;
        for (int i = 0; i < n; i++) {
            double dx = i - meanX;
            covariance += dx * (series.get(i) - meanY);
            variance += dx * dx;
        }
        return variance == 0d ? 0d : covariance / variance;
    }

    /** Exponentially weighted level of the series, i.e. the smoothed "current" value. */
    public static double level(List<Double> series) {
        if (series.isEmpty()) {
            return 0d;
        }
        double level = series.get(0);
        for (int i = 1; i < series.size(); i++) {
            level = SMOOTHING * series.get(i) + (1 - SMOOTHING) * level;
        }
        return level;
    }

    /** Projects {@code horizon} further steps, never returning negative values. */
    public static List<Double> forecast(List<Double> series, int horizon) {
        List<Double> projection = new ArrayList<>(horizon);
        if (series.isEmpty() || horizon <= 0) {
            return projection;
        }
        double level = level(series);
        double slope = slope(series);
        for (int step = 1; step <= horizon; step++) {
            projection.add(Math.max(0d, level + slope * step));
        }
        return projection;
    }

    /** Sum of the next {@code horizon} projected steps. */
    public static double forecastTotal(List<Double> series, int horizon) {
        return forecast(series, horizon).stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * Back-test accuracy in percent: the last {@code holdout} observations are re-predicted from the
     * preceding history and scored as {@code 100 - MAPE}, clamped to [0, 100].
     */
    public static double backTestAccuracy(List<Double> series, int holdout) {
        if (series.size() < holdout * 2 || holdout <= 0) {
            return 0d;
        }
        List<Double> history = series.subList(0, series.size() - holdout);
        List<Double> actual = series.subList(series.size() - holdout, series.size());
        List<Double> predicted = forecast(history, holdout);
        double errorSum = 0d;
        int counted = 0;
        for (int i = 0; i < holdout; i++) {
            double a = actual.get(i);
            if (a == 0d) {
                continue;
            }
            errorSum += Math.abs(a - predicted.get(i)) / a;
            counted++;
        }
        if (counted == 0) {
            return 0d;
        }
        return clamp(100d - (errorSum / counted) * 100d);
    }

    /** Percentage change of the second half of the series against the first half. */
    public static double trendPercent(List<Double> series) {
        if (series.size() < 2) {
            return 0d;
        }
        int half = series.size() / 2;
        double first = mean(series.subList(0, half));
        double second = mean(series.subList(half, series.size()));
        if (first == 0d) {
            return second == 0d ? 0d : 100d;
        }
        return (second - first) / first * 100d;
    }

    /**
     * Reorder quantity covering the lead time plus a safety buffer, less what is already on hand.
     */
    public static int recommendedReorderQuantity(double dailyDemand,
                                                 int leadTimeDays,
                                                 int safetyDays,
                                                 int stockOnHand) {
        double target = dailyDemand * (leadTimeDays + safetyDays);
        return (int) Math.max(0, Math.ceil(target - stockOnHand));
    }

    public static double mean(List<Double> values) {
        return values.isEmpty() ? 0d : values.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
    }

    public static double clamp(double value) {
        return Math.max(0d, Math.min(100d, value));
    }
}
