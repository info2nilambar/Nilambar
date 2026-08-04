package com.nilambar.erp.service.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.Address;
import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.Store;
import com.nilambar.erp.repository.StoreRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeliveryServiceTest {

    private static final double STORE_LAT = 12.9756;
    private static final double STORE_LNG = 77.6050;

    private DeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        ErpProperties properties = new ErpProperties();
        properties.getDelivery().setRadiusKm(5);
        deliveryService = new DeliveryService(Mockito.mock(StoreRepository.class), properties);
    }

    @Test
    void addressInsideRadiusGetsHomeDelivery() {
        DeliveryQuote quote = deliveryService.quote(addressAt(12.9900, 77.6100), List.of(store(STORE_LAT, STORE_LNG)));

        assertThat(quote.homeDeliveryAvailable()).isTrue();
        assertThat(quote.fulfilmentType()).isEqualTo(FulfilmentType.HOME_DELIVERY);
        assertThat(quote.distanceKm()).isLessThanOrEqualTo(5);
    }

    @Test
    void addressOutsideRadiusFallsBackToStorePickup() {
        DeliveryQuote quote = deliveryService.quote(addressAt(13.0900, 77.7000), List.of(store(STORE_LAT, STORE_LNG)));

        assertThat(quote.homeDeliveryAvailable()).isFalse();
        assertThat(quote.fulfilmentType()).isEqualTo(FulfilmentType.STORE_PICKUP);
        assertThat(quote.distanceKm()).isGreaterThan(5);
        assertThat(quote.message()).contains("beyond the 5 km home-delivery radius");
    }

    @Test
    void nearestOfSeveralStoresDecidesEligibility() {
        Store far = store(STORE_LAT, STORE_LNG);
        Store near = store(13.0850, 77.6980);

        DeliveryQuote quote = deliveryService.quote(addressAt(13.0900, 77.7000), List.of(far, near));

        assertThat(quote.nearestStore()).isSameAs(near);
        assertThat(quote.homeDeliveryAvailable()).isTrue();
    }

    @Test
    void radiusBoundaryIsInclusive() {
        // ~4.99 km due north of the store.
        DeliveryQuote quote = deliveryService.quote(addressAt(STORE_LAT + 0.0449, STORE_LNG),
                List.of(store(STORE_LAT, STORE_LNG)));

        assertThat(quote.distanceKm()).isLessThanOrEqualTo(5);
        assertThat(quote.homeDeliveryAvailable()).isTrue();
    }

    private Address addressAt(double latitude, double longitude) {
        Address address = new Address();
        address.setLatitude(latitude);
        address.setLongitude(longitude);
        return address;
    }

    private Store store(double latitude, double longitude) {
        Store store = new Store();
        store.setName("Store " + latitude);
        store.setLatitude(latitude);
        store.setLongitude(longitude);
        return store;
    }
}
