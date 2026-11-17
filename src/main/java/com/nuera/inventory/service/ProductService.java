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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(ProductRepository productRepository, InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public ProductResponse createProduct(ProductRequest request) {
        if (request.getSku() == null || request.getSku().isBlank()) {
            throw new InvalidOperationException("Product SKU cannot be empty");
        }
        if (productRepository.existsBySku(request.getSku().trim())) {
            throw new InvalidOperationException("Product with SKU '" + request.getSku() + "' already exists");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Product price must be greater than zero");
        }

        Product product = new Product(
                request.getSku().trim(),
                request.getName().trim(),
                request.getDescription(),
                request.getPrice()
        );

        Product savedProduct = productRepository.save(product);

        // Initialize inventory tracking for the new product
        Inventory inventory = new Inventory(savedProduct.getId(), savedProduct.getSku(), 0, 0);
        inventoryRepository.save(inventory);

        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Product price must be greater than zero");
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());

        Product updatedProduct = productRepository.save(product);
        return toResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        inventoryRepository.deleteByProductId(id);
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String name) {
        if (name == null || name.isBlank()) {
            return getAllProducts();
        }
        return productRepository.findByNameContainingIgnoreCase(name.trim()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
