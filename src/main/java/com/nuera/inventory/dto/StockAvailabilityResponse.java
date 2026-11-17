package com.nuera.inventory.dto;

public class StockAvailabilityResponse {

    private Long productId;
    private Integer availableQuantity;
    private Integer requestedQuantity;
    private boolean available;

    public StockAvailabilityResponse() {
    }

    public StockAvailabilityResponse(Long productId, Integer availableQuantity, Integer requestedQuantity, boolean available) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
        this.requestedQuantity = requestedQuantity;
        this.available = available;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
