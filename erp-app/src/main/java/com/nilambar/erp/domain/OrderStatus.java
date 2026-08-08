package com.nilambar.erp.domain;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    READY_FOR_PICKUP,
    PICKED_UP,
    CANCELLED;

    public OrderStatus next(FulfilmentType fulfilmentType) {
        return switch (this) {
            case PLACED -> CONFIRMED;
            case CONFIRMED -> fulfilmentType == FulfilmentType.HOME_DELIVERY ? OUT_FOR_DELIVERY : READY_FOR_PICKUP;
            case OUT_FOR_DELIVERY -> DELIVERED;
            case READY_FOR_PICKUP -> PICKED_UP;
            default -> this;
        };
    }
}
