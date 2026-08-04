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

    // JSP EL resolves JavaBean accessors only; record components are not visible to it.

    public boolean isHomeDeliveryAvailable() {
        return homeDeliveryAvailable;
    }

    public Store getNearestStore() {
        return nearestStore;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getRadiusKm() {
        return radiusKm;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public FulfilmentType getFulfilmentType() {
        return fulfilmentType;
    }

    public String getMessage() {
        return message;
    }
}
