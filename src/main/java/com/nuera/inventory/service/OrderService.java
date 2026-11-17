package com.nuera.inventory.service;

import com.nuera.inventory.dto.*;
import com.nuera.inventory.entity.Order;
import com.nuera.inventory.entity.OrderItem;
import com.nuera.inventory.entity.OrderStatus;
import com.nuera.inventory.entity.Product;
import com.nuera.inventory.exception.InsufficientStockException;
import com.nuera.inventory.exception.InvalidOperationException;
import com.nuera.inventory.exception.ResourceNotFoundException;
import com.nuera.inventory.repository.InventoryRepository;
import com.nuera.inventory.repository.OrderRepository;
import com.nuera.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        InventoryRepository inventoryRepository,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new InvalidOperationException("Customer name cannot be empty");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOperationException("Order must contain at least one item");
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderNumber, request.getCustomerName().trim(), OrderStatus.CONFIRMED, BigDecimal.ZERO);

        // Pre-validation pass: verify all products exist and sufficient stock is available
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new InvalidOperationException("Item quantity must be greater than zero for product ID: " + itemReq.getProductId());
            }

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemReq.getProductId()));

            var inventory = inventoryRepository.findByProductId(product.getId())
                    .orElseThrow(() -> new InsufficientStockException(product.getId(), itemReq.getQuantity(), 0));

            if (inventory.getAvailableQuantity() < itemReq.getQuantity()) {
                throw new InsufficientStockException(product.getId(), itemReq.getQuantity(), inventory.getAvailableQuantity());
            }
        }

        // Deduction & Item aggregation pass: all checks passed
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemReq.getProductId()));

            // Deduct inventory atomically
            inventoryService.reduceStock(product.getId(), itemReq.getQuantity(), "Order: " + orderNumber);

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = new OrderItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    itemReq.getQuantity(),
                    subtotal
            );
            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getStatus(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt()
        );
    }
}
