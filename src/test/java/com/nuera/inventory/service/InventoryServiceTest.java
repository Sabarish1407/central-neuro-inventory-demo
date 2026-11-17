package com.nuera.inventory.service;

import com.nuera.inventory.dto.InventoryResponse;
import com.nuera.inventory.dto.StockAvailabilityResponse;
import com.nuera.inventory.entity.Inventory;
import com.nuera.inventory.entity.Product;
import com.nuera.inventory.exception.InsufficientStockException;
import com.nuera.inventory.exception.InvalidOperationException;
import com.nuera.inventory.exception.ResourceNotFoundException;
import com.nuera.inventory.repository.InventoryRepository;
import com.nuera.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = new Product("SKU-200", "Mechanical Keyboard", "RGB Gaming", new BigDecimal("79.99"));
        product.setId(2L);

        inventory = new Inventory(2L, "SKU-200", 25, 0);
        inventory.setId(10L);
    }

    @Test
    @DisplayName("Should successfully retrieve current inventory")
    void getInventory_success() {
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(inventory));

        InventoryResponse response = inventoryService.getInventory(2L);

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(2L);
        assertThat(response.getSku()).isEqualTo("SKU-200");
        assertThat(response.getAvailableQuantity()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when querying inventory for nonexistent product")
    void getInventory_productNotFound_throwsException() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID: 99");
    }

    @Test
    @DisplayName("Should add stock and increase available quantity")
    void addStock_success() {
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryResponse response = inventoryService.addStock(2L, 15, "New batch shipment");

        assertThat(response).isNotNull();
        assertThat(response.getAvailableQuantity()).isEqualTo(40);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("Should reject stock addition when quantity is zero or negative")
    void addStock_zeroOrNegative_throwsException() {
        assertThatThrownBy(() -> inventoryService.addStock(2L, 0, "Zero"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> inventoryService.addStock(2L, -10, "Negative"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than zero");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reduce stock when available quantity is sufficient")
    void reduceStock_success() {
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryResponse response = inventoryService.reduceStock(2L, 10, "Order fulfillment");

        assertThat(response).isNotNull();
        assertThat(response.getAvailableQuantity()).isEqualTo(15);
        verify(inventoryRepository).save(inventory);
    }

    @Test
    @DisplayName("Should throw InsufficientStockException and reject reduction when requested quantity exceeds available stock")
    void reduceStock_insufficientStock_throwsException() {
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.reduceStock(2L, 30, "Excessive reduction"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 30, available 25");

        verify(inventoryRepository, never()).save(any());
        assertThat(inventory.getAvailableQuantity()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should reject stock reduction when quantity is zero or negative")
    void reduceStock_zeroOrNegative_throwsException() {
        assertThatThrownBy(() -> inventoryService.reduceStock(2L, 0, "Invalid"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> inventoryService.reduceStock(2L, -5, "Invalid"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than zero");

        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should check availability correctly when stock is sufficient")
    void checkAvailability_sufficient_returnsTrue() {
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(inventory));

        StockAvailabilityResponse response = inventoryService.checkAvailability(2L, 20);

        assertThat(response.isAvailable()).isTrue();
        assertThat(response.getAvailableQuantity()).isEqualTo(25);
        assertThat(response.getRequestedQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should check availability correctly when stock is insufficient")
    void checkAvailability_insufficient_returnsFalse() {
        when(inventoryRepository.findByProductId(2L)).thenReturn(Optional.of(inventory));

        StockAvailabilityResponse response = inventoryService.checkAvailability(2L, 50);

        assertThat(response.isAvailable()).isFalse();
        assertThat(response.getAvailableQuantity()).isEqualTo(25);
        assertThat(response.getRequestedQuantity()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should reject availability check with zero or negative requested quantity")
    void checkAvailability_invalidQuantity_throwsException() {
        assertThatThrownBy(() -> inventoryService.checkAvailability(2L, 0))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than zero");
    }
}
