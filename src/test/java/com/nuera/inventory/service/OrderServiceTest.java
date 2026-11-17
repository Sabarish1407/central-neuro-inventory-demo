package com.nuera.inventory.service;

import com.nuera.inventory.dto.CreateOrderRequest;
import com.nuera.inventory.dto.OrderItemRequest;
import com.nuera.inventory.dto.OrderResponse;
import com.nuera.inventory.entity.Inventory;
import com.nuera.inventory.entity.Order;
import com.nuera.inventory.entity.OrderStatus;
import com.nuera.inventory.entity.Product;
import com.nuera.inventory.exception.InsufficientStockException;
import com.nuera.inventory.exception.InvalidOperationException;
import com.nuera.inventory.exception.ResourceNotFoundException;
import com.nuera.inventory.repository.InventoryRepository;
import com.nuera.inventory.repository.OrderRepository;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderService orderService;

    private Product product1;
    private Product product2;
    private Inventory inventory1;
    private Inventory inventory2;

    @BeforeEach
    void setUp() {
        product1 = new Product("SKU-MOUSE", "Gaming Mouse", "16000 DPI", new BigDecimal("50.00"));
        product1.setId(10L);

        product2 = new Product("SKU-PAD", "Mouse Pad", "Extended Desk Mat", new BigDecimal("20.00"));
        product2.setId(20L);

        inventory1 = new Inventory(10L, "SKU-MOUSE", 15, 0);
        inventory2 = new Inventory(20L, "SKU-PAD", 8, 0);
    }

    @Test
    @DisplayName("Should successfully create multi-item order, calculate exact total, and deduct stock")
    void createOrder_success() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Alice Smith",
                List.of(
                        new OrderItemRequest(10L, 2),  // 2 * 50.00 = 100.00
                        new OrderItemRequest(20L, 3)   // 3 * 20.00 =  60.00
                )
        );

        when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product2));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory1));
        when(inventoryRepository.findByProductId(20L)).thenReturn(Optional.of(inventory2));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(100L);
            return o;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerName()).isEqualTo("Alice Smith");
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("160.00");
        assertThat(response.getItems()).hasSize(2);

        // Verify stock was deducted for both products
        verify(inventoryService).reduceStock(eq(10L), eq(2), anyString());
        verify(inventoryService).reduceStock(eq(20L), eq(3), anyString());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should reject order and prevent stock deduction when any product has insufficient stock")
    void createOrder_insufficientStock_throwsExceptionAndDoesNotDeduct() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Bob Jones",
                List.of(
                        new OrderItemRequest(10L, 2),
                        new OrderItemRequest(20L, 10)  // Available is only 8
                )
        );

        when(productRepository.findById(10L)).thenReturn(Optional.of(product1));
        when(inventoryRepository.findByProductId(10L)).thenReturn(Optional.of(inventory1));
        when(productRepository.findById(20L)).thenReturn(Optional.of(product2));
        when(inventoryRepository.findByProductId(20L)).thenReturn(Optional.of(inventory2));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 10, available 8");

        // Verify NO stock deductions or order saves occurred
        verify(inventoryService, never()).reduceStock(any(), anyInt(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject order creation when customer name is blank")
    void createOrder_emptyCustomer_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest("   ", List.of(new OrderItemRequest(10L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Customer name cannot be empty");
    }

    @Test
    @DisplayName("Should reject order creation when items list is empty")
    void createOrder_emptyItems_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest("Alice", List.of());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when an ordered product does not exist")
    void createOrder_productNotFound_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest("Charlie", List.of(new OrderItemRequest(999L, 1)));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with ID: 999");

        verify(inventoryService, never()).reduceStock(any(), anyInt(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve order by ID when order exists")
    void getOrderById_success() {
        Order order = new Order("ORD-TEST1234", "Diana", OrderStatus.CONFIRMED, new BigDecimal("50.00"));
        order.setId(1L);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNumber()).isEqualTo("ORD-TEST1234");
        assertThat(response.getCustomerName()).isEqualTo("Diana");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when order does not exist")
    void getOrderById_notFound_throwsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with ID: 999");
    }
}
