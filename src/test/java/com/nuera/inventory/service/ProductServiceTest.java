package com.nuera.inventory.service;

import com.nuera.inventory.dto.ProductRequest;
import com.nuera.inventory.dto.ProductResponse;
import com.nuera.inventory.dto.ProductUpdateRequest;
import com.nuera.inventory.entity.Inventory;
import com.nuera.inventory.entity.Product;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product("SKU-100", "Wireless Mouse", "Ergonomic 2.4G", new BigDecimal("29.99"));
        sampleProduct.setId(1L);
    }

    @Test
    @DisplayName("Should successfully create product and initialize 0-level inventory")
    void createProduct_success() {
        ProductRequest request = new ProductRequest("SKU-100", "Wireless Mouse", "Ergonomic 2.4G", new BigDecimal("29.99"));

        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(new Inventory(1L, "SKU-100", 0, 0));

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-100");
        assertThat(response.getName()).isEqualTo("Wireless Mouse");
        assertThat(response.getPrice()).isEqualByComparingTo("29.99");

        verify(productRepository).save(any(Product.class));
        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Should reject product creation when SKU already exists")
    void createProduct_duplicateSku_throwsException() {
        ProductRequest request = new ProductRequest("SKU-100", "Wireless Mouse", "Ergonomic", new BigDecimal("29.99"));

        when(productRepository.existsBySku("SKU-100")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already exists");

        verify(productRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject product creation when price is zero or negative")
    void createProduct_invalidPrice_throwsException() {
        ProductRequest requestZero = new ProductRequest("SKU-101", "Keyboard", "Mechanical", BigDecimal.ZERO);
        ProductRequest requestNegative = new ProductRequest("SKU-102", "Keyboard", "Mechanical", new BigDecimal("-5.00"));

        assertThatThrownBy(() -> productService.createProduct(requestZero))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> productService.createProduct(requestNegative))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than zero");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return product by ID when product exists")
    void getProductById_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product does not exist")
    void getProductById_notFound_throwsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID: 999");
    }

    @Test
    @DisplayName("Should update product details and price correctly")
    void updateProduct_success() {
        ProductUpdateRequest updateRequest = new ProductUpdateRequest("Wireless Mouse Pro", "Updated ergonomic design", new BigDecimal("39.99"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.updateProduct(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(productRepository).save(sampleProduct);
        assertThat(sampleProduct.getName()).isEqualTo("Wireless Mouse Pro");
        assertThat(sampleProduct.getPrice()).isEqualByComparingTo("39.99");
    }

    @Test
    @DisplayName("Should reject product update with invalid price")
    void updateProduct_invalidPrice_throwsException() {
        ProductUpdateRequest updateRequest = new ProductUpdateRequest("Mouse", "Desc", BigDecimal.ZERO);
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    @DisplayName("Should delete product and associated inventory")
    void deleteProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        productService.deleteProduct(1L);

        verify(inventoryRepository).deleteByProductId(1L);
        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("Should search products by name case-insensitively")
    void searchProducts_filtersByName() {
        when(productRepository.findByNameContainingIgnoreCase("mouse"))
                .thenReturn(List.of(sampleProduct));

        List<ProductResponse> results = productService.searchProducts("mouse");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Wireless Mouse");
    }
}
