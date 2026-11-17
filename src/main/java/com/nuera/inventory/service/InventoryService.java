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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        Inventory inventory = findOrCreateInventoryForProduct(productId);
        return toResponse(inventory);
    }

    @Transactional(readOnly = true)
    public StockAvailabilityResponse checkAvailability(Long productId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new InvalidOperationException("Requested quantity must be greater than zero");
        }

        Inventory inventory = findOrCreateInventoryForProduct(productId);
        boolean available = inventory.getAvailableQuantity() >= requestedQuantity;

        return new StockAvailabilityResponse(
                productId,
                inventory.getAvailableQuantity(),
                requestedQuantity,
                available
        );
    }

    public InventoryResponse addStock(Long productId, int quantity, String reason) {
        if (quantity <= 0) {
            throw new InvalidOperationException("Stock addition quantity must be greater than zero");
        }

        Inventory inventory = findOrCreateInventoryForProduct(productId);
        int updatedQuantity = inventory.getAvailableQuantity() + quantity;
        inventory.setAvailableQuantity(updatedQuantity);

        Inventory saved = inventoryRepository.save(inventory);
        return toResponse(saved);
    }

    public InventoryResponse reduceStock(Long productId, int quantity, String reason) {
        if (quantity <= 0) {
            throw new InvalidOperationException("Stock reduction quantity must be greater than zero");
        }

        Inventory inventory = findOrCreateInventoryForProduct(productId);

        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, inventory.getAvailableQuantity());
        }

        int updatedQuantity = inventory.getAvailableQuantity() - quantity;
        if (updatedQuantity < 0) {
            throw new InvalidOperationException("Inventory stock cannot become negative");
        }

        inventory.setAvailableQuantity(updatedQuantity);
        Inventory saved = inventoryRepository.save(inventory);
        return toResponse(saved);
    }

    private Inventory findOrCreateInventoryForProduct(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));
                    Inventory newInv = new Inventory(product.getId(), product.getSku(), 0, 0);
                    return inventoryRepository.save(newInv);
                });
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return new InventoryResponse(
                inventory.getProductId(),
                inventory.getSku(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getLastUpdated()
        );
    }
}
