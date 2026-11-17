# Central Neuro Inventory Demo (`central-neuro-inventory-demo`)

An independent, production-grade Inventory Management System built with Spring Boot, Spring Data JPA, and H2 database. This repository serves as a target validation application for Central Neuro's autonomous analysis, test generation, failure detection, code modification, and deployment verification capabilities.

---

## 1. Overview & Architecture

The application implements a clean layered architecture with domain segregation:

```
[ HTTP REST Clients / Central Neuro Orchestrator ]
                        │
                        ▼
┌────────────────────────────────────────────────────────┐
│               REST Controllers Layer                   │
│   ProductController | InventoryController | OrderCtrl  │
└───────────────────────┬────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────┐
│                 Service Layer                          │
│   ProductService    | InventoryService    | OrderServ  │
│       └── Validation, Stock Math & Transactions ──┘    │
└───────────────────────┬────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────┐
│            Spring Data JPA Repositories                │
│   ProductRepository | InventoryRepo | OrderRepo        │
└───────────────────────┬────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────┐
│             H2 In-Memory / File Database               │
└────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack

* **Java:** 21 (Eclipse Adoptium OpenJDK 21)
* **Framework:** Spring Boot 4.x
* **Data Access:** Spring Data JPA / Hibernate ORM
* **Validation:** Jakarta Validation (`spring-boot-starter-validation`)
* **Monitoring:** Spring Boot Actuator (`/actuator/health`)
* **Database:** H2 Database Engine (embedded in-memory by default)
* **Testing:** JUnit 5, Mockito, AssertJ, Spring Boot Test, Spring WebMvc Test
* **Containerization:** Multi-stage Dockerfile, Docker Compose (`compose.yaml`)
* **CI/CD:** Jenkins Declarative Pipeline (`Jenkinsfile`)
* **Build Tool:** Apache Maven 3.10 with included Maven Wrapper (`mvnw`, `mvnw.cmd`)

---

## 3. Business Rules

1. **Product Price:** Must be strictly greater than zero (`price > 0.00`).
2. **Stock Non-Negativity:** Inventory available stock can never be negative under any condition.
3. **Adjustment Quantities:** Additions and manual reductions require quantities $\ge 1$.
4. **Order Sizing:** Order item quantities must be greater than zero.
5. **Inventory Availability Gate:** Orders cannot exceed current available stock.
6. **Order Total Formula:** $\text{Total} = \sum (\text{Product Price} \times \text{Item Quantity})$.
7. **Atomic Rollback Guarantee:** If an order requests multiple items and any single item has insufficient stock, the entire transaction is rolled back; no items have their stock reduced.
8. **Instant Visibility:** All stock deductions and additions are reflected immediately in subsequent queries.

---

## 4. REST API Endpoints

| Method | Endpoint | Description | Success Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/products` | Create a new product and initialize stock at 0 | `201 Created` |
| `GET` | `/api/products` | Retrieve all products | `200 OK` |
| `GET` | `/api/products/{id}` | Retrieve single product by ID | `200 OK` |
| `PUT` | `/api/products/{id}` | Update product name, description, or price | `200 OK` |
| `DELETE`| `/api/products/{id}` | Delete product and associated inventory | `204 No Content` |
| `GET` | `/api/products/search?name=` | Search products by name (case-insensitive) | `200 OK` |
| `POST` | `/api/inventory/{productId}/add` | Add stock quantity | `200 OK` |
| `POST` | `/api/inventory/{productId}/reduce` | Deduct stock quantity | `200 OK` |
| `GET` | `/api/inventory/{productId}` | Retrieve current stock details | `200 OK` |
| `GET` | `/api/inventory/{productId}/availability?quantity=` | Check if requested stock is available | `200 OK` |
| `POST` | `/api/orders` | Place multi-item order atomically | `201 Created` |
| `GET` | `/api/orders/{orderId}` | Retrieve order details with items | `200 OK` |
| `GET` | `/api/orders` | Retrieve list of placed orders | `200 OK` |
| `GET` | `/actuator/health` | Application health status | `200 OK` |

---

## 5. Sample API Requests & Responses

### A. Create Product
**Request:** `POST /api/products`
```json
{
  "sku": "SKU-MON-01",
  "name": "UltraWide Monitor 34in",
  "description": "Curved 144Hz HDR Display",
  "price": 499.99
}
```
**Response:** `201 Created`
```json
{
  "id": 1,
  "sku": "SKU-MON-01",
  "name": "UltraWide Monitor 34in",
  "description": "Curved 144Hz HDR Display",
  "price": 499.99,
  "createdAt": "2026-11-17T13:30:00.000",
  "updatedAt": "2026-11-17T13:30:00.000"
}
```

### B. Add Stock
**Request:** `POST /api/inventory/1/add`
```json
{
  "quantity": 25,
  "reason": "Warehouse supplier shipment"
}
```
**Response:** `200 OK`
```json
{
  "productId": 1,
  "sku": "SKU-MON-01",
  "availableQuantity": 25,
  "reservedQuantity": 0,
  "lastUpdated": "2026-11-17T13:31:00.000"
}
```

### C. Create Order
**Request:** `POST /api/orders`
```json
{
  "customerName": "Acme Logistics Corp",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```
**Response:** `201 Created`
```json
{
  "orderId": 1,
  "orderNumber": "ORD-E3F8A1B2",
  "customerName": "Acme Logistics Corp",
  "status": "CONFIRMED",
  "totalAmount": 999.98,
  "items": [
    {
      "productId": 1,
      "productName": "UltraWide Monitor 34in",
      "unitPrice": 499.99,
      "quantity": 2,
      "subtotal": 999.98
    }
  ],
  "createdAt": "2026-11-17T13:32:00.000"
}
```

### D. Rejection on Insufficient Stock
**Request:** `POST /api/orders` (requesting 50 units when only 23 remain)
**Response:** `400 Bad Request`
```json
{
  "timestamp": "2026-11-17T13:33:00.000",
  "status": 400,
  "error": "Insufficient Stock",
  "message": "Insufficient stock for product ID 1: requested 50, available 23",
  "path": "/api/orders"
}
```

---

## 6. Execution Instructions (Windows PowerShell)

### Local Build & Baseline Verification
Execute from the repository root:
```powershell
# Clean, compile, and run full test suite
.\mvnw.cmd clean test

# Package JAR file
.\mvnw.cmd package -DskipTests
```

### Run Application Locally
```powershell
# Run using Spring Boot Maven plugin on port 8081
.\mvnw.cmd spring-boot:run
```
Once started, verify health via PowerShell:
```powershell
Invoke-RestMethod -Uri http://localhost:8081/actuator/health
```

---

## 7. Docker & Container Deployment

### Build Docker Image
```powershell
docker build -t central-neuro-inventory-demo:latest .
```

### Run with Docker Compose
```powershell
docker compose up -d
```
Inspect logs and health:
```powershell
docker compose logs -f
curl http://localhost:8081/actuator/health
```

---

## 8. Jenkins CI/CD Pipeline

The included `Jenkinsfile` provides an enterprise declarative pipeline:
1. **Checkout:** Checks out source code from SCM.
2. **Compile & Test:** Executes unit and integration tests, publishing Surefire JUnit reports.
3. **Package Application:** Builds the executable Spring Boot fat JAR.
4. **Docker Build:** Builds the multi-stage image.
5. **Publish Image:** Optional stage controlled by `PUBLISH_IMAGE` parameter using external credentials (`ghcr-credentials`).

---

## 9. Environment Profiles

* `application.yml`: Base configuration (Port 8081, Graceful shutdown, Actuator exposure).
* `application-dev.yml`: Local developer profile with H2 web console enabled at `/h2-console`.
* `application-test.yml`: Isolated test profile with `create-drop` schema.
* `application-docker.yml`: Production container profile using file-backed persistent H2 storage.
* `.env.example`: Template for environment-specific variables.
