package com.nilambar.erp.service.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void distanceBetweenIdenticalPointsIsZero() {
        assertThat(GeoUtils.haversineKm(12.9756, 77.6050, 12.9756, 77.6050)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAboutOneHundredAndElevenKilometres() {
        assertThat(GeoUtils.haversineKm(0, 0, 1, 0)).isCloseTo(111.19, org.assertj.core.data.Offset.offset(0.2));
    }

    @Test
    void knownCityPairMatchesPublishedDistance() {
        // Bengaluru MG Road -> Hebbal is roughly 6.7 km as the crow flies.
        double distance = GeoUtils.haversineKm(12.9756, 77.6050, 13.0358, 77.5970);
        assertThat(distance).isCloseTo(6.75, org.assertj.core.data.Offset.offset(0.3));
    }

    @Test
    void distanceIsSymmetric() {
        double forward = GeoUtils.haversineKm(12.90, 77.50, 12.99, 77.62);
        double backward = GeoUtils.haversineKm(12.99, 77.62, 12.90, 77.50);
        assertThat(forward).isEqualTo(backward);
    }
}
