package com.nuera.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockAdjustmentRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Adjustment quantity must be at least 1")
    private Integer quantity;

    private String reason;

    public StockAdjustmentRequest() {
    }

    public StockAdjustmentRequest(Integer quantity, String reason) {
        this.quantity = quantity;
        this.reason = reason;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
