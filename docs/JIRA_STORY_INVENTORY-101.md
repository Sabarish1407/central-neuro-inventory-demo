# Jira Story: INVENTORY-101

## Story Title
Implement Atomic Multi-Item Order Placement and Real-Time Stock Deductions

## Business Description
Our e-commerce warehouse currently requires reliable, atomic stock management when customers place orders containing multiple items. If any item in the order has insufficient inventory, the entire order must be rejected immediately without deducting stock for earlier items in the basket. In addition, stock reductions and replenishments must be reflected instantly across all inventory and availability queries.

## Repository Details
* **Repository Name:** `central-neuro-inventory-demo`
* **Local Repository Path:** `c:\Users\sabar\Documents\Sabarish\Project\Tech_swift\nuera-agent\central-neuro-inventory-demo`
* **Remote Repository URL:** `[CONFIGURED_REPOSITORY_URL_PLACEHOLDER]` *(Local baseline repository ready for publishing)*
* **Base Branch:** `main`
* **Target / Feature Branch:** `feature/INVENTORY-101-order-stock-management`

---

## User Story
**As an** Inventory Operations Manager  
**I want** orders containing one or more products to validate available stock and atomically deduct quantities upon successful placement  
**So that** we prevent overselling, avoid negative inventory levels, and ensure customers are never charged for out-of-stock items.

---

## Functional Requirements
1. **Product Management:**
   * Create, read, update, delete, and search products.
   * Enforce unique SKU and price strictly greater than zero.
   * Auto-initialize inventory record at quantity 0 upon product creation.
2. **Stock Management:**
   * Support stock addition (`/api/inventory/{productId}/add`) with positive quantity.
   * Support stock reduction (`/api/inventory/{productId}/reduce`) with quantity validation.
   * Prevent negative inventory under all conditions.
   * Provide stock availability check (`/api/inventory/{productId}/availability?quantity=`).
3. **Order Placement:**
   * Accept orders with customer name and a list of order items (`productId`, `quantity`).
   * Calculate exact order total: `sum(product.price * item.quantity)`.
   * Enforce atomic transaction rollback if any item in the order has insufficient stock.
   * Return HTTP 201 Created on success, HTTP 400 Bad Request with error details on failure.
4. **Health Check:**
   * Expose `/actuator/health` returning `{"status": "UP"}`.

---

## Acceptance Criteria
- [ ] `AC-1`: Placing an order with all items in stock deducts the exact quantities from inventory and returns status `CONFIRMED` with HTTP 201.
- [ ] `AC-2`: Placing an order where any item quantity exceeds available stock returns HTTP 400 with `Insufficient Stock` error.
- [ ] `AC-3`: When a multi-item order fails due to insufficient stock on any item, previously evaluated items must NOT suffer partial stock deductions (all-or-nothing rollback).
- [ ] `AC-4`: The order total amount must equal the exact sum of each item's unit price multiplied by quantity.
- [ ] `AC-5`: Attempting to adjust stock with quantity $\le 0$ returns HTTP 400 Bad Request.
- [ ] `AC-6`: All baseline unit and integration tests execute and pass via Maven.

---

## Technical Requirements
* **Language & Framework:** Java 21, Spring Boot 4.x
* **Persistence:** Spring Data JPA with H2 in-memory / file database
* **Transaction Boundary:** Spring `@Transactional(rollbackFor = Exception.class)` on `OrderService.createOrder`
* **Containerization:** Multi-stage Dockerfile and `compose.yaml` exposing port 8081
* **CI/CD:** Jenkinsfile declarative pipeline with test archiving and optional image publication

---

## Definition of Done (DoD)
1. Source code compiles cleanly without errors.
2. Unit tests cover services, calculations, and exception paths.
3. Integration tests verify REST endpoints, database persistence, and transactional rollback.
4. Baseline test suite passes with 0 failures (`mvn clean test`).
5. Application runs locally and responds to actuator health checks.
6. Dockerfile and Jenkinsfile are validated.
7. Committed to Git with clean commit history on `main` and branched to `feature/INVENTORY-101-order-stock-management`.
