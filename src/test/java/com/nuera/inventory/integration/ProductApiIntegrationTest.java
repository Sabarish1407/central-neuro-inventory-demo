package com.nuera.inventory.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuera.inventory.dto.ProductRequest;
import com.nuera.inventory.dto.ProductUpdateRequest;
import com.nuera.inventory.entity.Product;
import com.nuera.inventory.repository.ProductRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/products creates product and returns 201 Created")
    void createProduct_returns201() throws Exception {
        ProductRequest request = new ProductRequest("SKU-INT-1", "Monitor 27in", "4K IPS", new BigDecimal("349.99"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sku").value("SKU-INT-1"))
                .andExpect(jsonPath("$.name").value("Monitor 27in"))
                .andExpect(jsonPath("$.price").value(349.99));
    }

    @Test
    @DisplayName("POST /api/products with duplicate SKU returns 400 Bad Request")
    void createProduct_duplicateSku_returns400() throws Exception {
        productRepository.save(new Product("SKU-INT-DUP", "Mouse", "Optic", new BigDecimal("19.99")));

        ProductRequest duplicateRequest = new ProductRequest("SKU-INT-DUP", "Another Mouse", "Optic", new BigDecimal("25.00"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Operation"))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/products with invalid price returns 400 Validation Failed")
    void createProduct_invalidPrice_returns400() throws Exception {
        ProductRequest invalidRequest = new ProductRequest("SKU-INT-NEG", "Free Mouse", "Free", new BigDecimal("0.00"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/products/{id} returns product when exists")
    void getProductById_returns200() throws Exception {
        Product saved = productRepository.save(new Product("SKU-INT-GET", "Webcam HD", "1080p 60fps", new BigDecimal("79.50")));

        mockMvc.perform(get("/api/products/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-INT-GET"))
                .andExpect(jsonPath("$.name").value("Webcam HD"));
    }

    @Test
    @DisplayName("GET /api/products/search?name= returns matching products")
    void searchProducts_returnsMatchingList() throws Exception {
        productRepository.save(new Product("SKU-INT-S1", "Mechanical Keyboard Pro", "Blue Switch", new BigDecimal("99.00")));
        productRepository.save(new Product("SKU-INT-S2", "Membrane Keyboard", "Quiet", new BigDecimal("29.00")));
        productRepository.save(new Product("SKU-INT-S3", "Wireless Mouse", "2.4G", new BigDecimal("35.00")));

        mockMvc.perform(get("/api/products/search").param("name", "keyboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("PUT /api/products/{id} updates product and returns 200 OK")
    void updateProduct_returns200() throws Exception {
        Product saved = productRepository.save(new Product("SKU-INT-UPD", "Old Headset", "Wired", new BigDecimal("49.99")));

        ProductUpdateRequest updateRequest = new ProductUpdateRequest("New Headset ANC", "Wireless ANC", new BigDecimal("89.99"));

        mockMvc.perform(put("/api/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Headset ANC"))
                .andExpect(jsonPath("$.price").value(89.99));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} deletes product and returns 204 No Content")
    void deleteProduct_returns204() throws Exception {
        Product saved = productRepository.save(new Product("SKU-INT-DEL", "To Delete", "Temp", new BigDecimal("10.00")));

        mockMvc.perform(delete("/api/products/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", saved.getId()))
                .andExpect(status().isNotFound());
    }
}
