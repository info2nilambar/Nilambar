package com.nilambar.erp.service.delivery;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.Address;
import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.Store;
import com.nilambar.erp.repository.StoreRepository;
import com.nilambar.erp.service.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private final StoreRepository storeRepository;
    private final ErpProperties properties;

    public DeliveryService(StoreRepository storeRepository, ErpProperties properties) {
        this.storeRepository = storeRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public DeliveryQuote quoteFor(Address address) {
        List<Store> stores = storeRepository.findByActiveTrue();
        if (stores.isEmpty()) {
            throw new BusinessException("No active store is configured; delivery cannot be quoted.");
        }
        return quote(address, stores);
    }

    /**
     * Pure decision logic, kept separate from persistence so it can be unit tested directly.
     */
    public DeliveryQuote quote(Address address, List<Store> stores) {
        Store nearest = stores.stream()
                .min(Comparator.comparingDouble(store -> distanceKm(address, store)))
                .orElseThrow(() -> new BusinessException("No active store is configured; delivery cannot be quoted."));

        double distance = distanceKm(address, nearest);
        double radius = properties.getDelivery().getRadiusKm();

        if (distance <= radius) {
            BigDecimal fee = deliveryFee(distance);
            return new DeliveryQuote(true, nearest, distance, radius, fee, FulfilmentType.HOME_DELIVERY,
                    "Home delivery available from %s (%.2f km away) for a %s delivery charge."
                            .formatted(nearest.getName(), distance, fee.toPlainString()));
        }
        return new DeliveryQuote(false, nearest, distance, radius,
                properties.getDelivery().getPickupFee(), FulfilmentType.STORE_PICKUP,
                ("This address is %.2f km from the nearest store (%s), beyond the %.0f km home-delivery radius. "
                        + "Store pickup is available instead.")
                        .formatted(distance, nearest.getName(), radius));
    }

    /**
     * Charges one slab fee for every started slab of distance, so 0-1.5 km costs one slab,
     * 1.5-3 km costs two, and so on.
     */
    public BigDecimal deliveryFee(double distanceKm) {
        double slabKm = properties.getDelivery().getFeeSlabKm();
        BigDecimal perSlab = properties.getDelivery().getFeePerSlab();
        if (slabKm <= 0 || perSlab.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        int slabs = Math.max(1, (int) Math.ceil(distanceKm / slabKm - 1e-9));
        return perSlab.multiply(BigDecimal.valueOf(slabs)).setScale(2, RoundingMode.HALF_UP);
    }

    private double distanceKm(Address address, Store store) {
        return GeoUtils.haversineKm(address.getLatitude(), address.getLongitude(),
                store.getLatitude(), store.getLongitude());
    }
}
