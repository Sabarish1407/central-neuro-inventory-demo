package com.nuera.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuera.inventory.dto.ProductRequest;
import com.nuera.inventory.dto.StockAdjustmentRequest;
import com.nuera.inventory.repository.InventoryRepository;
import com.nuera.inventory.repository.ProductRepository;
import com.nuera.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InventoryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Long sampleProductId;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        productRepository.deleteAll();

        var p = productService.createProduct(new ProductRequest("SKU-INV-TEST", "Office Chair", "Ergonomic mesh", new BigDecimal("199.95")));
        sampleProductId = p.getId();
    }

    @Test
    @DisplayName("POST /api/inventory/{id}/add increments available stock")
    void addStock_increasesAvailableQuantity() throws Exception {
        StockAdjustmentRequest addReq = new StockAdjustmentRequest(30, "Warehouse shipment");

        mockMvc.perform(post("/api/inventory/{productId}/add", sampleProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(sampleProductId))
                .andExpect(jsonPath("$.availableQuantity").value(30));

        mockMvc.perform(get("/api/inventory/{productId}", sampleProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(30));
    }

    @Test
    @DisplayName("POST /api/inventory/{id}/reduce decrements available stock")
    void reduceStock_decrementsAvailableQuantity() throws Exception {
        StockAdjustmentRequest addReq = new StockAdjustmentRequest(50, "Stock replenishment");
        mockMvc.perform(post("/api/inventory/{productId}/add", sampleProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        StockAdjustmentRequest reduceReq = new StockAdjustmentRequest(15, "Customer sale");
        mockMvc.perform(post("/api/inventory/{productId}/reduce", sampleProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reduceReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(35));
    }

    @Test
    @DisplayName("POST /api/inventory/{id}/reduce with amount exceeding stock returns 400 Bad Request")
    void reduceStock_exceedingStock_returns400() throws Exception {
        StockAdjustmentRequest addReq = new StockAdjustmentRequest(10, "Initial stock");
        mockMvc.perform(post("/api/inventory/{productId}/add", sampleProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        StockAdjustmentRequest excessiveReq = new StockAdjustmentRequest(25, "Overdraw");
        mockMvc.perform(post("/api/inventory/{productId}/reduce", sampleProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Stock"));
    }

    @Test
    @DisplayName("GET /api/inventory/{id}/availability reports correct boolean flag")
    void checkAvailability_reportsCorrectStatus() throws Exception {
        mockMvc.perform(post("/api/inventory/{productId}/add", sampleProductId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockAdjustmentRequest(20, "Batch"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/inventory/{productId}/availability", sampleProductId).param("quantity", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(get("/api/inventory/{productId}/availability", sampleProductId).param("quantity", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
