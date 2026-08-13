package com.nilambar.erp.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ForecasterTest {

    @Test
    void slopeFollowsALinearSeries() {
        assertThat(Forecaster.slope(List.of(10d, 20d, 30d, 40d))).isCloseTo(10d, within());
        assertThat(Forecaster.slope(List.of(40d, 30d, 20d, 10d))).isCloseTo(-10d, within());
        assertThat(Forecaster.slope(List.of(5d))).isZero();
    }

    @Test
    void forecastProjectsTheTrendAndNeverGoesNegative() {
        List<Double> projection = Forecaster.forecast(List.of(10d, 20d, 30d, 40d), 3);

        assertThat(projection).hasSize(3);
        assertThat(projection.get(0)).isGreaterThan(30d);
        assertThat(projection.get(2)).isGreaterThan(projection.get(0));
        assertThat(Forecaster.forecast(List.of(40d, 20d, 5d, 1d), 6)).allSatisfy(value ->
                assertThat(value).isGreaterThanOrEqualTo(0d));
    }

    @Test
    void forecastTotalSumsTheHorizon() {
        List<Double> series = List.of(4d, 5d, 6d, 7d);

        assertThat(Forecaster.forecastTotal(series, 3))
                .isCloseTo(Forecaster.forecast(series, 3).stream().mapToDouble(Double::doubleValue).sum(), within());
    }

    @Test
    void backTestAccuracyIsHighForAPredictableSeriesAndBoundedForNoise() {
        List<Double> steady = List.of(20d, 20d, 20d, 20d, 20d, 20d, 20d, 20d, 20d, 20d, 20d, 20d);
        List<Double> noisy = List.of(5d, 90d, 3d, 120d, 1d, 88d, 4d, 130d, 2d, 95d, 6d, 140d);

        assertThat(Forecaster.backTestAccuracy(steady, 4)).isGreaterThan(90d);
        assertThat(Forecaster.backTestAccuracy(noisy, 4)).isBetween(0d, 100d);
        assertThat(Forecaster.backTestAccuracy(List.of(1d, 2d), 4)).isBetween(0d, 100d);
    }

    @Test
    void recommendedReorderQuantityCoversLeadTimeAndSafetyStock() {
        assertThat(Forecaster.recommendedReorderQuantity(10d, 5, 7, 20)).isEqualTo(100);
        assertThat(Forecaster.recommendedReorderQuantity(10d, 5, 7, 500)).isZero();
        assertThat(Forecaster.recommendedReorderQuantity(0d, 5, 7, 0)).isZero();
    }

    @Test
    void trendPercentComparesTheTwoHalvesOfTheSeries() {
        assertThat(Forecaster.trendPercent(List.of(10d, 10d, 20d, 20d))).isCloseTo(100d, within());
        assertThat(Forecaster.trendPercent(List.of(20d, 20d, 10d, 10d))).isCloseTo(-50d, within());
        assertThat(Forecaster.trendPercent(List.of())).isZero();
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(0.5d);
    }
}
