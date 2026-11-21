package com.nuera.inventory.integration;

import tools.jackson.databind.ObjectMapper;
import com.nuera.inventory.dto.*;
import com.nuera.inventory.repository.InventoryRepository;
import com.nuera.inventory.repository.OrderRepository;
import com.nuera.inventory.repository.ProductRepository;
import com.nuera.inventory.service.InventoryService;
import com.nuera.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Long itemOneId;
    private Long itemTwoId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();

        ProductResponse p1 = productService.createProduct(
                new ProductRequest("SKU-ORD-1", "Desk Lamp", "LED Dimmable", new BigDecimal("40.00")));
        ProductResponse p2 = productService.createProduct(
                new ProductRequest("SKU-ORD-2", "USB Hub", "7-Port Powered", new BigDecimal("30.00")));

        itemOneId = p1.getId();
        itemTwoId = p2.getId();

        inventoryService.addStock(itemOneId, 20, "Initial Stock");
        inventoryService.addStock(itemTwoId, 10, "Initial Stock");
    }

    @Test
    @DisplayName("POST /api/orders creates order, computes total, and reduces inventory atomically")
    void createOrder_success_deductsInventory_calculatesExactTotal() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Jane Doe",
                List.of(
                        new OrderItemRequest(itemOneId, 2),  // 2 * 40.00 = 80.00
                        new OrderItemRequest(itemTwoId, 3)   // 3 * 30.00 = 90.00
                )
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").isNumber())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(170.00))
                .andExpect(jsonPath("$.items", hasSize(2)));

        // Verify inventory reductions
        InventoryResponse inv1 = inventoryService.getInventory(itemOneId);
        InventoryResponse inv2 = inventoryService.getInventory(itemTwoId);
        assertThat(inv1.getAvailableQuantity()).isEqualTo(18);
        assertThat(inv2.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("POST /api/orders rejects order exceeding stock with 400 and keeps inventory intact")
    void createOrder_insufficientStock_returns400_andStockUnchanged() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Mark Spencer",
                List.of(new OrderItemRequest(itemTwoId, 25)) // Available is only 10
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Stock"));

        // Stock should remain unchanged
        InventoryResponse inv2 = inventoryService.getInventory(itemTwoId);
        assertThat(inv2.getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("POST /api/orders verifies transactional rollback when second item fails stock check")
    void transactionalRollback_multiItemFailure_leavesStockUnchanged() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Rollback Test",
                List.of(
                        new OrderItemRequest(itemOneId, 5),  // Available: 20
                        new OrderItemRequest(itemTwoId, 50)  // Available: 10 -> will fail!
                )
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient Stock"));

        // Both stocks must remain untouched
        InventoryResponse inv1 = inventoryService.getInventory(itemOneId);
        InventoryResponse inv2 = inventoryService.getInventory(itemTwoId);
        assertThat(inv1.getAvailableQuantity()).isEqualTo(20);
        assertThat(inv2.getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("GET /api/orders/{id} retrieves placed order details")
    void getOrderById_returnsOrderDetails() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Sarah Connor",
                List.of(new OrderItemRequest(itemOneId, 1))
        );

        String responseJson = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        OrderResponse created = objectMapper.readValue(responseJson, OrderResponse.class);

        mockMvc.perform(get("/api/orders/{id}", created.getOrderId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Sarah Connor"))
                .andExpect(jsonPath("$.totalAmount").value(40.00));
    }
}
