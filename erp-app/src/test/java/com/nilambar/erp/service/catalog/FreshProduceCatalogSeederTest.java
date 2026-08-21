package com.nilambar.erp.service.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.StockMovement;
import com.nilambar.erp.domain.StockMovementType;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.repository.StockMovementRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FreshProduceCatalogSeederTest {

    private ProductRepository productRepository;
    private StockMovementRepository stockMovementRepository;
    private FreshProduceCatalogSeeder seeder;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        stockMovementRepository = mock(StockMovementRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2024-05-01T08:00:00Z"), ZoneOffset.UTC);
        seeder = new FreshProduceCatalogSeeder(productRepository, stockMovementRepository, clock);
    }

    @Test
    void addsTenVegetablesAndTenFruits() {
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        seeder.run(null);

        List<Product> saved = capture();
        assertThat(saved).hasSize(20);
        assertThat(saved).filteredOn(p -> "Vegetables".equals(p.getCategory())).hasSize(10);
        assertThat(saved).filteredOn(p -> "Fruits".equals(p.getCategory())).hasSize(10);
        assertThat(saved).extracting(Product::getSku).doesNotHaveDuplicates();
    }

    @Test
    void savedProductsAreCatalogReady() {
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        seeder.run(null);

        assertThat(capture()).allSatisfy(product -> {
            assertThat(product.getName()).isNotBlank();
            assertThat(product.getDescription()).isNotBlank();
            assertThat(product.getPrice()).isGreaterThan(BigDecimal.ZERO);
            assertThat(product.getUnitCost()).isGreaterThan(BigDecimal.ZERO).isLessThan(product.getPrice());
            assertThat(product.getStockQuantity()).isGreaterThan(product.getReorderLevel());
            assertThat(product.getReorderLevel()).isPositive();
            assertThat(product.getImagePath()).isEqualTo("/images/products/" + product.getSku() + ".png");
            assertThat(product.getLastRestockedAt()).isEqualTo(Instant.parse("2024-05-01T08:00:00Z"));
            assertThat(product.isActive()).isTrue();
        });
    }

    @Test
    void recordsOpeningStockForTheLedger() {
        when(productRepository.findBySku(anyString())).thenReturn(Optional.empty());

        seeder.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockMovement>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockMovementRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(20)
                .allSatisfy(movement -> {
                    assertThat(movement.getMovementType()).isEqualTo(StockMovementType.IN);
                    assertThat(movement.getQuantity()).isEqualTo(movement.getProduct().getStockQuantity());
                    assertThat(movement.getReference()).isEqualTo("CATALOG-" + movement.getProduct().getSku());
                });
    }

    @Test
    void isIdempotentWhenProduceAlreadySeeded() {
        when(productRepository.findBySku(anyString())).thenReturn(Optional.of(new Product()));

        seeder.run(null);

        verify(productRepository, never()).saveAll(anyIterable());
        verify(stockMovementRepository, never()).saveAll(anyIterable());
    }

    private List<Product> capture() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
