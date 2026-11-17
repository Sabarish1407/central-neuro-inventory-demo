# Central Neuro Inventory Demo — Baseline Verification

**Verification date:** 2026-11-18 (Asia/Kolkata, UTC+05:30)  
**Overall verdict: FAIL**

The required Maven builds and all 46 existing tests passed, and the application passed the requested API smoke checks. However, an isolated live API boundary test showed that adding a valid quantity can overflow the inventory `Integer` and persist a negative stock count. That violates the core non-negative-inventory rule. Docker image construction and Jenkins parser validation could not be completed because the Docker daemon and Jenkins/Groovy tooling were unavailable.

## Repository identity and state

| Item | Observed |
|---|---|
| Repository | `central-neuro-inventory-demo` |
| Branch | `poc/openrewrite-springboot-34-to-35` |
| Commit | `00441f484873cc8135524bc3699ac30c249ec652` |
| Initial working tree | Clean (`git status --short` returned no entries) |
| Final working tree | Clean before creating this report; only this requested report was added |

The Jira document names `main` and `feature/INVENTORY-101-order-stock-management`; the checked-out branch is neither. The report is the only requested deliverable added. Maven's prescribed `clean` and `package` commands regenerated the ignored `target/` build outputs; no application source or configuration was edited.

## Repository structure inspected

Tracked root files: `.env.example`, `.gitignore`, `.mvn/wrapper/maven-wrapper.properties`, `Dockerfile`, `Jenkinsfile`, `README.md`, `compose.yaml`, `mvnw`, `mvnw.cmd`, and `pom.xml`.

The application has 31 main Java source files:

- `src/main/java/com/nuera/inventory/`: application entry point.
- `controller/`: `ProductController`, `InventoryController`, `OrderController`.
- `service/`: `ProductService`, `InventoryService`, `OrderService`.
- `repository/`: `ProductRepository`, `InventoryRepository`, `OrderRepository`.
- `entity/`: `Product`, `Inventory`, `Order`, `OrderItem`, `OrderStatus`.
- `dto/`: product create/update/response, inventory/availability/adjustment, order create/item/response, and error response DTOs.
- `exception/`: global handler plus insufficient-stock, invalid-operation, and not-found exceptions.
- `util/`: `LegacyJava17Patterns`.

Configuration includes four main resources (`application.yml`, `application-dev.yml`, `application-test.yml`, `application-docker.yml`) and two test resources (`application.yml`, `application-test.yml`). There is one documentation file, `docs/JIRA_STORY_INVENTORY-101.md`.

Eight test classes (46 tests total) are organized as:

- Integration: `ActuatorHealthIntegrationTest`, `ProductApiIntegrationTest`, `InventoryApiIntegrationTest`, `OrderApiIntegrationTest`.
- Services: `ProductServiceTest`, `InventoryServiceTest`, `OrderServiceTest`.
- Utility: `LegacyJava17PatternsTest`.

The checkout also contained `target/` build outputs and empty `runtime_err.log` and `runtime_test.log`; these are not tracked source files.

## Technology and configuration

| Component | Observed version or setting |
|---|---|
| Java runtime | Eclipse Temurin OpenJDK `21.0.12.1+1-LTS` |
| Spring Boot | `3.5.16` (POM parent and runtime banner) |
| Maven Wrapper | Wrapper `3.3.4`, downloading Apache Maven `3.10.0` |
| Spring Boot starters | Web, Data JPA, Validation, Actuator: `3.5.16` |
| H2 | `2.3.232` runtime |
| Hibernate ORM | `6.6.53.Final` |
| JUnit/Spring test stack | `spring-boot-starter-test` `3.5.16`; JUnit Platform/Surefire ran the tests |
| Maven plugins observed | Compiler `3.14.1`, Surefire `3.5.6`, Spring Boot Maven plugin `3.5.16` |
| Compile release | Java 21 |

**Specification/documentation mismatch:** `README.md` and the Jira technical requirements say Spring Boot 4.x, but `pom.xml`, Maven output, and the running application identify Spring Boot 3.5.16. The README also names Maven 3.10, matching the wrapper.

Profile configuration inspected:

- Base `application.yml`: port 8081, default `dev` profile, in-memory H2, JPA `ddl-auto: update`, actuator health/info/metrics exposed.
- `dev`: H2 console enabled at `/h2-console`, SQL/debug logging enabled.
- `test`: port 0, isolated in-memory H2, `create-drop`.
- `docker`: port 8081, file-backed H2 under `/app/data`, `create-drop` is not used.
- Test-resource base and `test` configs both use port 0 and in-memory H2 with `create-drop`.

The three REST controllers delegate to three transactional services and Spring Data JPA repositories. Entities use JPA lifecycle timestamps; product SKU and inventory product ID have database uniqueness constraints. Request DTOs use Jakarta validation, including a positive price minimum and positive adjustment/order quantities. Errors are mapped to structured HTTP responses by the global exception handler.

## Build and test results

| Command | Result | Measured wall duration | Maven reported duration |
|---|---:|---:|---:|
| `.\mvnw.cmd clean test` | **PASS**, exit 0 | 27.068 s | 23.442 s |
| `.\mvnw.cmd package -DskipTests` | **PASS**, exit 0 | 8.311 s | 5.583 s |

Test summary: **46 run, 0 failures, 0 errors, 0 skipped**.

| Test class | Tests | Failures / errors / skipped |
|---|---:|---:|
| `ActuatorHealthIntegrationTest` | 1 | 0 / 0 / 0 |
| `InventoryApiIntegrationTest` | 4 | 0 / 0 / 0 |
| `OrderApiIntegrationTest` | 4 | 0 / 0 / 0 |
| `ProductApiIntegrationTest` | 7 | 0 / 0 / 0 |
| `InventoryServiceTest` | 10 | 0 / 0 / 0 |
| `OrderServiceTest` | 7 | 0 / 0 / 0 |
| `ProductServiceTest` | 9 | 0 / 0 / 0 |
| `LegacyJava17PatternsTest` | 4 | 0 / 0 / 0 |

The test command compiled 31 main and 8 test source files. Non-fatal output included a deprecated-API note in `LegacyJava17Patterns`, Hibernate's unnecessary explicit H2 dialect warning, Spring's open-in-view warning, and Mockito/JDK dynamic-agent warnings.

## Runtime API smoke tests

The packaged JAR was started on port 8081 with the `test` profile and a distinct in-memory database URL. Runtime data was isolated and disappeared with the process; the process was stopped after testing.

| Check | Observed result |
|---|---|
| `GET /actuator/health` | HTTP 200, `status=UP` |
| `POST /api/products`, then `GET /api/products/{id}` | Product created and retrieved |
| Inventory for a newly created product | Initial `availableQuantity=0` |
| Inventory add 10, reduce 2, then get | Immediate value 8 |
| Multi-item successful order | `CONFIRMED`, HTTP 201; total `46.75` for 2 × 12.50 plus 3 × 7.25; subsequent inventory reads were 6 and 7 |
| Order exceeding available stock | HTTP 400; stock remained unchanged |
| Multi-item failure after a first deduction | Request contained two lines for the same product (5 then 4 with 8 in stock): HTTP 400 and stock remained 8 after the second deduction failed. This exercises rollback after a write, unlike the existing integration test's pre-validation failure. |
| Invalid stock addition 0 / reduction -1 | HTTP 400 / HTTP 400 |
| Order quantity 0 / product price 0 | HTTP 400 / HTTP 400 |
| Integer-boundary inventory addition | Adding `2147483647`, then adding `1`, returned `availableQuantity=-2147483648` |
| Reduction after overflow | HTTP 400; the negative inventory state was not repaired |

### Business-rule verification matrix

The supplied overview enumerates eight numbered rules although the task asks for nine. This matrix covers those eight and adds the explicit SKU-uniqueness requirement from the Jira acceptance details. Product inventory initialization is noted separately.

| Rule | Evidence and executable-test coverage | Assessment |
|---|---|---|
| Product price strictly positive | Service tests reject zero and negative; product API integration test rejects zero; live API zero-price request returned 400. | **Verified for tested values** |
| Inventory never negative | Existing service test rejects a reduction exceeding stock, but there is no overflow boundary test. Live API accepted `MAX_INT + 1` across two additions and persisted `-2147483648`. | **FAIL — reproducible invariant violation** |
| Stock adjustment quantities are positive | Service tests cover zero/negative additions and reductions; live API checks returned 400 for zero addition and negative reduction. | **Verified** |
| Order item quantity is positive | DTO/service validation exists and live API rejected quantity zero with 400. No existing test explicitly covers zero/negative/null order item quantity. | **Runtime verified; automated regression test missing** |
| Stock availability and order stock gate | Availability unit/integration tests cover sufficient/insufficient values; order integration test rejects excess stock and checks stock remains unchanged. | **Verified** |
| Order total is the sum of price × quantity | Order service and API integration tests assert computed totals; live API returned 46.75 for the tested two-item order. | **Verified** |
| Multi-item order is atomic | Existing integration test checks unchanged stock on a failed second item, but `OrderService` pre-validates every line before the deduction loop, so that test does not trigger a rollback after an earlier deduction. The isolated live duplicate-line test did trigger that path and preserved stock. | **Runtime verified; existing rollback test does not exercise rollback itself** |
| Changes are immediately visible | Inventory integration tests query after addition; order integration and live checks query after deductions; live reads reflected each change. | **Verified** |
| SKU uniqueness | Product service and API integration tests reject a duplicate SKU; `Product` also declares a unique database constraint. | **Verified for sequential duplicate creation** |

**Product initialization note:** Runtime product creation was followed by an inventory query returning zero. The service test verifies that the inventory repository is called but does not assert the saved entity's quantity is zero.

## Docker and Jenkins

### Docker

- Installed CLI reported Docker `29.9.3`; Docker Compose reported `v5.7.0`.
- `docker compose -f compose.yaml config --quiet` succeeded (exit 0), validating Compose syntax/config rendering.
- `docker info --format '{{.ServerVersion}}'` failed because the Docker Desktop Linux engine named pipe was unavailable.
- `docker build -t central-neuro-inventory-demo:baseline-verification .` failed (exit 1) for the same unavailable daemon. **Dockerfile build success is therefore unverified**, not a source-level build failure.
- The Dockerfile is a two-stage Temurin 21 Alpine image, builds with the Maven wrapper, and runs on port 8081 using the `docker` profile. The Compose service maps 8081 and persists `/app/data`.
- External requirements include Docker engine access, Maven Central, and the `eclipse-temurin:21-jdk-alpine` / `eclipse-temurin:21-jre-alpine` base images. The Dockerfile's dependency-prefetch step ends in `|| true`, so prefetch errors are masked; the subsequent package step remains the build gate.
- Compose sets `DB_URL`; the inspected Docker profile directly configures `spring.datasource.url`, so the supplied `DB_URL` environment variable does not override that value under the standard Spring property names.

### Jenkins

- The declarative pipeline was inspected but **not parser-validated**: neither `jenkins` nor `groovy` is installed, and no Jenkins server/CLI was available.
- Stages present: Checkout, Compile & Test (publishes Surefire XML with `allowEmptyResults: true`), Package Application (archives/fingerprints JAR), Docker Build, and optional Publish Image.
- `PUBLISH_IMAGE` defaults to false. Publishing requires the configured Jenkins credentials ID (default `ghcr-credentials`) and registry access; no credentials were inspected or used.
- Jenkins environment must provide a configured JDK tool named `jdk-21`, Docker CLI/daemon, SCM checkout, and compatible Pipeline, JUnit, Credentials Binding, and Workspace Cleanup plugins. The `sh` steps require a Unix-like agent; no external deployment or image publication was triggered.

## Findings and verdict rationale

1. **Blocking behavior defect:** `InventoryService.addStock` adds signed 32-bit quantities without overflow checking. A valid pair of stock additions can wrap the stored count below zero. This fails the explicit invariant that inventory cannot be negative and is independently reproducible at the live API.
2. **Missing automated order-quantity test:** validation is present and the live zero-quantity request returned 400, but no existing executable test covers this rule.
3. **Rollback test limitation:** the existing integration scenario's failure occurs in the pre-validation pass; it does not demonstrate rollback of earlier writes. The runtime duplicate-line case exercised the actual later failure and succeeded, but should not be mistaken for an existing automated regression test.
4. **Build/verification limitations:** Docker image build was blocked by the unavailable daemon; Jenkins declarative syntax was not parser-validated due missing tooling.
5. **Documentation mismatch:** README and Jira expect Spring Boot 4.x; the project builds and runs on Spring Boot 3.5.16.

The builds, existing tests, Compose validation, and requested API scenarios pass. The reproducible negative-stock state violates a central business invariant, so the overall baseline is **FAIL**, not PASS WITH LIMITATIONS.

## Commands executed

Repository and tool inspection:

```powershell
git status --short
git branch --show-current
git rev-parse HEAD
git ls-files
java -version
.\mvnw.cmd -version
```

Required builds and dependency inspection:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package -DskipTests
.\mvnw.cmd dependency:tree -Dscope=compile
.\mvnw.cmd dependency:tree -Dscope=runtime '-Dincludes=com.h2database:h2,org.springframework.boot:spring-boot-starter-*' -Dstyle.color=never
& .\mvnw.cmd 'dependency:tree' '-Dscope=runtime' '-Dincludes=com.h2database:h2,org.springframework.boot:spring-boot-starter-*' '-Dstyle.color=never'
```

The first optional filtered dependency-tree invocation above failed because PowerShell/Maven parsed the unquoted `-Dstyle.color=never` as an invalid lifecycle phase (`.color=never`). The quoted invocation succeeded. This did not affect either required build.

Runtime and container checks:

```powershell
java -jar 'target\central-neuro-inventory-demo-0.0.1-SNAPSHOT.jar' '--spring.profiles.active=test' '--server.port=8081' '--spring.datasource.url=jdbc:h2:mem:baseline_smoke;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
docker info --format '{{.ServerVersion}}'
docker compose -f compose.yaml config --quiet
docker build -t central-neuro-inventory-demo:baseline-verification .
docker --version
docker compose version
```

HTTP calls were made with PowerShell `Invoke-RestMethod` against `http://localhost:8081` using isolated SKU prefixes `BASE-VERIFY-*`. Routes exercised: `/actuator/health`, `/api/products` (POST and GET by ID), `/api/inventory/{id}` (GET, `/add`, `/reduce`), and `/api/orders` (successful, insufficient-stock, late-failing duplicate-line, and invalid-quantity requests). Inventory quantities used included 10, 2, 5, 4, 0, -1, 2147483647, and 1; order totals and status codes are recorded above.
