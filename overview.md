# Sun Travels Application Overview

## Purpose

This file describes the project structure and main implementation boundaries for use by another coding agent making further application changes.

## Technology summary

- Backend: Java 25, Spring Boot 3.5.7, Maven
- Backend security: Spring Security, JWT 0.12.6
- Database: MySQL 8, Spring Data JPA/Hibernate
- Frontend: Angular 20, TypeScript, standalone components
- Frontend test runner: Vitest through Angular unit-test builder
- Backend test runner: JUnit 5, Mockito, Spring Boot Test

## Repository hierarchy

```text
assignment1/
├── backend/
│   ├── pom.xml
│   └── main/
│       ├── java/com/suntravels/
│       │   ├── SunTravelsApplication.java
│       │   ├── controller/
│       │   │   ├── AdminContractController.java
│       │   │   ├── AgentSearchController.java
│       │   │   └── AuthController.java
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   ├── JwtService.java
│       │   │   └── SearchService.java
│       │   ├── repository/
│       │   │   ├── CallCenterAgentRepository.java
│       │   │   ├── ContractAdministratorRepository.java
│       │   │   ├── ContractRepository.java
│       │   │   └── UserRepository.java
│       │   ├── model/
│       │   │   ├── User.java
│       │   │   ├── Role.java
│       │   │   ├── Contract.java
│       │   │   ├── RoomType.java
│       │   │   ├── ContractAdministrator.java
│       │   │   ├── CallCenterAgent.java
│       │   │   ├── SearchRequest.java
│       │   │   ├── RoomRequest.java
│       │   │   └── SearchResult.java
│       │   ├── dto/
│       │   │   ├── AuthResponse.java
│       │   │   ├── LoginRequest.java
│       │   │   └── SignupRequest.java
│       │   └── config/
│       │       ├── SecurityConfig.java
│       │       └── JwtAuthenticationFilter.java
│       └── resources/
│           ├── application.properties
│           └── application-mysql.properties
├── frontend/
│   ├── angular.json
│   ├── package.json
│   ├── tsconfig.json
│   ├── tsconfig.app.json
│   ├── tsconfig.spec.json
│   ├── main.ts
│   ├── index.html
│   └── app/
│       ├── app.component.ts
│       ├── app.config.ts
│       ├── app.routes.ts
│       ├── contract-form.component.ts
│       ├── contract-list.component.ts
│       ├── search.component.ts
│       ├── core/
│       │   ├── services/auth.service.ts
│       │   ├── interceptors/jwt.interceptor.ts
│       │   ├── models/auth.model.ts
│       │   └── guards/
│       │       ├── auth.guard.ts
│       │       ├── guest.guard.ts
│       │       └── role.guard.ts
│       ├── features/
│       │   ├── auth/auth.component.ts
│       │   ├── admin/admin-dashboard.component.ts
│       │   └── agent/agent-dashboard.component.ts
│       └── search-contract/search-contract.component.ts
├── testing/
│   ├── backend-unit/
│   │   ├── Business Logic/BusinessLogicTest.java
│   │   ├── REST Controllers/RESTControllerTest.java
│   │   └── exception handling/ExceptionHandlingTest.java
│   ├── DB/DbTest.java
│   ├── api/
│   │   ├── authentication/AuthenticationTest.java
│   │   ├── cors/CorsTest.java
│   │   └── spring-db/SpringDbTest.java
│   └── frontend-unit/
│       ├── components/admin-components.spec.ts
│       ├── pipes/display-pipes.spec.ts
│       ├── pipes/contract-and-search-forms.reactive.spec.ts
│       ├── route-guards/auth-and-role.guards.spec.ts
│       └── services/auth-and-jwt.service.spec.ts
├── submissions/
│   ├── db.sql
│   ├── ER.drawio
│   ├── ER.png
│   ├── test_plan.txt
│   ├── user_stories.txt
│   └── notes.txt
├── README.md
├── explain.md
└── overview.md
```

## Runtime flow

### Authentication flow

```text
Angular AuthComponent
  -> AuthService
  -> POST /api/auth/signup or /api/auth/login
  -> AuthController
  -> AuthService
  -> UserRepository and role profile repository
  -> JwtService
  -> JWT stored by Angular
```

### Protected API flow

```text
Angular component
  -> HttpClient
  -> jwt.interceptor
  -> Authorization: Bearer <JWT>
  -> JwtAuthenticationFilter
  -> SecurityConfig role check
  -> REST controller
```

### Contract creation flow

```text
Admin ContractFormComponent
  -> POST /api/admin/contracts
  -> AdminContractController
  -> ContractRepository.save(...)
  -> JPA cascades RoomType inserts
  -> MySQL
```

### Search and pricing flow

```text
Agent SearchComponent
  -> POST /api/agent/search
  -> AgentSearchController
  -> SearchService
  -> ContractRepository.findValidContracts(...)
  -> Evaluate room count and maximum adults
  -> Calculate markup and total price
  -> Return SearchResult[]
```

## API surface

| Method | Path | Role | Purpose |
|---|---|---|---|
| POST | `/api/auth/signup` | Public | Register administrator or agent |
| POST | `/api/auth/login` | Public | Authenticate and return JWT |
| POST | `/api/admin/contracts` | `CONTRACT_ADMIN` | Create a contract |
| GET | `/api/admin/contracts` | `CONTRACT_ADMIN` | List contracts |
| GET | `/api/admin/contracts/search` | `CONTRACT_ADMIN` | Search contracts by hotel or date |
| POST | `/api/agent/search` | `CALL_CENTER_AGENT` | Search contracts and calculate prices |

## Main domain rules

- A stay is valid only when `checkIn <= validTo` and `checkOut >= validFrom`.
- The implementation uses `checkOut = checkIn + number of nights`.
- A room type qualifies only when it has enough rooms for every requested room entry.
- Every room entry must fit within the room type's `maxAdults`.
- Price per result is:

  ```text
  roomType.price × totalAdults × noOfNights × (1 + contract.markup / 100)
  ```

- Prices are rounded to two decimal places using `HALF_UP`.

## Entity relationships

- `User` to `ContractAdministrator`: one-to-one.
- `User` to `CallCenterAgent`: one-to-one.
- `ContractAdministrator` to `Contract`: one-to-many.
- `Contract` to `RoomType`: one-to-many with cascade and orphan removal.

## Configuration

- Backend default database settings: `backend/main/resources/application.properties`
- Optional MySQL profile: `backend/main/resources/application-mysql.properties`
- Database: `suntravels`
- Default JDBC user: `root`
- Default JDBC password: empty
- Backend port: `8080`
- Frontend port: `4200`
- CORS allowed origin: `http://localhost:4200`

Environment variables accepted by the MySQL profile/configuration:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Change guidance

- Put HTTP request/response behavior in controllers.
- Put reusable business rules in services.
- Keep entity persistence operations in repositories or JPA entities.
- Keep JWT parsing and authorization out of components.
- Preserve the existing role names: `CONTRACT_ADMIN` and `CALL_CENTER_AGENT`.
- Preserve BigDecimal arithmetic for hotel prices and markup calculations.
- Add a backend test for any search or pricing rule change.
- Add a frontend unit test for any guard, interceptor, form, or HTTP component change.
- Use `mvn test` for Java tests and `npm test -- --watch=false` for frontend tests.

## Known implementation notes

- The search service currently omits room types that do not fit instead of returning them with `Unavailable`.
- The assignment requires available/unavailable statuses, so displaying unavailable room types may require changing `SearchService` to return unsuccessful candidates as well.
- The assignment room examples include grouped room quantities; the frontend currently stores one `RoomRequest` per room entry.
- The application uses a local MySQL database and JPA `update`/`create-drop` behavior depending on the active configuration.
