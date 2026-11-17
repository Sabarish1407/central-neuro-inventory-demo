package com.nuera.inventory.dto;

import java.time.LocalDateTime;

public class InventoryResponse {

    private Long productId;
    private String sku;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private LocalDateTime lastUpdated;

    public InventoryResponse() {
    }

    public InventoryResponse(Long productId, String sku, Integer availableQuantity,
                             Integer reservedQuantity, LocalDateTime lastUpdated) {
        this.productId = productId;
        this.sku = sku;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.lastUpdated = lastUpdated;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
