package com.nuera.inventory.controller;

import com.nuera.inventory.dto.InventoryResponse;
import com.nuera.inventory.dto.StockAdjustmentRequest;
import com.nuera.inventory.dto.StockAvailabilityResponse;
import com.nuera.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @GetMapping("/{productId}/availability")
    public ResponseEntity<StockAvailabilityResponse> checkAvailability(@PathVariable Long productId,
                                                                       @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(inventoryService.checkAvailability(productId, quantity));
    }

    @PostMapping("/{productId}/add")
    public ResponseEntity<InventoryResponse> addStock(@PathVariable Long productId,
                                                      @Valid @RequestBody StockAdjustmentRequest request) {
        InventoryResponse response = inventoryService.addStock(productId, request.getQuantity(), request.getReason());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/reduce")
    public ResponseEntity<InventoryResponse> reduceStock(@PathVariable Long productId,
                                                         @Valid @RequestBody StockAdjustmentRequest request) {
        InventoryResponse response = inventoryService.reduceStock(productId, request.getQuantity(), request.getReason());
        return ResponseEntity.ok(response);
    }
}
