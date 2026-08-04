package com.nilambar.erp.service.delivery;

import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.Store;
import java.math.BigDecimal;

public record DeliveryQuote(
        boolean homeDeliveryAvailable,
        Store nearestStore,
        double distanceKm,
        double radiusKm,
        BigDecimal fee,
        FulfilmentType fulfilmentType,
        String message) {

    public String getFormattedDistance() {
        return String.format("%.2f", distanceKm);
    }
}
