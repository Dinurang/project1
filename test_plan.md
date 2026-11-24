# Sun Travels Test Plan

## 1. Purpose

This test plan defines how the Sun Travels application is verified against its functional requirements, security rules, database behavior, and user workflows.
It is based on the implemented application and the tests in the `testing/` directory.

## 2. System under test

Sun Travels is a role-based hotel contract and room quotation application.

- **Frontend:** Angular 20 and TypeScript, served on `http://localhost:4200`.
- **Backend:** Java 25, Spring Boot, Spring Security, JWT, and Spring Data JPA,
  served on `http://localhost:8080`.
- **Database:** MySQL 8 database `suntravels`.
- **Roles:**
  - `CONTRACT_ADMIN`: creates, views, and searches hotel contracts.
  - `CALL_CENTER_AGENT`: searches rooms and calculates customer quotations.

## 3. Test objectives

The tests verify that:

1. Users can register and authenticate with the correct role.
2. Protected endpoints reject unauthenticated and incorrectly authorized users.
3. Contract and room type data is persisted correctly.
4. Contract date ranges are applied correctly to searches.
5. Room availability is calculated from room count and adult capacity.
6. Available and unavailable room types are returned with the correct status.
7. Marked-up prices are calculated using the required formula and rounding.
8. Invalid requests return safe, useful HTTP responses.
9. CORS allows the configured frontend and rejects unauthorized origins.
10. Angular forms, guards, services, interceptors, components, and display
    formatting behave correctly.

## 4. Test scope

### In scope

- Signup and login for both roles.
- JWT creation, storage, attachment, expiry, and rejection.
- Admin contract creation, listing, and searching.
- Agent room search and quotation calculation.
- Available and unavailable room results.
- Database entities, repositories, relationships, and cascading room types.
- Date boundary, expired, future, and mid-stay contract cases.
- Validation, malformed JSON, invalid dates, and repository failures.
- Angular form validation, route guards, authentication service, JWT
  interceptor, admin components, and display pipes.
- CORS preflight and actual cross-origin requests.

### Out of scope

- Production load, stress, and penetration testing.
- Testing third-party MySQL infrastructure or browser compatibility beyond the
  configured Angular test environment.
- Payment, booking, cancellation, and inventory reservation workflows, which
  are not implemented.

## 5. Test environment

### Required software

- Java 25
- Maven 3.9+
- Node.js 22+
- npm 10+
- MySQL 8

### Configuration

The backend uses the following defaults:

- URL: `jdbc:mysql://localhost:3306/suntravels`
- Username: `root`
- Password: empty
- Backend port: `8080`
- Frontend port: `4200`

`DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` may be supplied as environment
variables when the defaults are not appropriate.

### Test data

Database tests create administrator, agent, contract, and room type records.
Integration tests populate realistic contracts with different validity periods,
room counts, adult capacities, markups, and duplicate identifiers.

## 6. Test approach

### Test summary

| Test type | Contained test cases | Framework / tool | Status |
|---|---|---|---|
| Backend unit — business logic | `TC-BL-01` to `TC-BL-07`: availability, nights, adult/room counts, markup, rounding, and null input | JUnit 5, Mockito, AssertJ | **Pass — 7/7** |
| Backend unit — REST controllers | `TC-CTRL-01` to `TC-CTRL-07`: contract CRUD/search, agent search, auth success/error responses | JUnit 5, Mockito, Spring MockMvc | **Pass — 7/7** |
| Backend unit — exception handling | `TC-EX-01` to `TC-EX-07`: missing profiles, repository failures, malformed JSON, invalid dates, auth errors, empty search | JUnit 5, Mockito, Spring MockMvc | **Pass — 7/7** |
| Database / repository | `TC-DB-01` to `TC-DB-15`: persistence, relationships, cascade, date boundaries, availability, listing, and search | JUnit 5, Spring Boot Test, Hibernate/JPA, MySQL | **Pass — 15/15** |
| API — authentication / authorization | `TC-AUTH-01` to `TC-AUTH-07`: JWT, login, role access, duplicate/invalid signup | JUnit 5, Spring Security, MockMvc | **Pass — 7/7** |
| API — CORS | `TC-CORS-01` to `TC-CORS-06`: allowed origin, preflight, disallowed origin/method/header | JUnit 5, Spring Security, MockMvc | **Pass — 6/6** |
| API + database integration | `TC-E2E-01` to `TC-E2E-07`: signup, contracts, search, unavailable inventory, duplicate and cross-role safety | JUnit 5, Spring Boot Test, MockMvc, JPA/Hibernate, MySQL | **Pass — 7/7** |
| Frontend — components | `TC-FE-COMP-01` to `TC-FE-COMP-03`: admin dashboard and contract list states | Angular TestBed, Vitest, JSDOM | **Pass — 3/3** |
| Frontend — reactive forms | `TC-FE-FORM-01` to `TC-FE-FORM-05`: contract/search validation and dynamic room rows | Angular TestBed, Vitest, JSDOM | **Pass — 5/5** |
| Frontend — display pipes | `TC-FE-PIPE-01` to `TC-FE-PIPE-03`: price, null, zero, and date formatting | Angular DecimalPipe/DatePipe, Vitest | **Pass — 3/3** |
| Frontend — route guards | `TC-FE-GUARD-01` to `TC-FE-GUARD-05`: authentication and role-based routing | Angular TestBed, Vitest, JSDOM | **Pass — 5/5** |
| Frontend — auth service/interceptor | `TC-FE-SVC-01` to `TC-FE-SVC-04`: login, logout, session, and JWT headers | Angular TestBed, HttpClient Testing, Vitest | **Pass — 4/4** |

### Test cases covered by each test file

#### Backend business logic: `testing/backend-unit/Business Logic/BusinessLogicTest.java`

- `TC-BL-01`: Marks a room type `Available` only when room count and every room's adult capacity are sufficient.
- `TC-BL-02`: Marks room types `Unavailable` when room count or adult capacity is insufficient.
- `TC-BL-03`: Calculates checkout from check-in plus nights and includes nights in the price.
- `TC-BL-04`: Calculates price accurately from the number of adults in one room.
- `TC-BL-05`: Calculates price accurately across multiple requested rooms.
- `TC-BL-06`: Calculates combined room, adult, night, and markup cost using `HALF_UP` rounding.
- `TC-BL-07`: Returns an empty result immediately for incomplete or null search fields without querying the database.

#### REST controllers: `testing/backend-unit/REST Controllers/RESTControllerTest.java`

- `TC-CTRL-01`: Creates a contract with the authenticated administrator and verifies returned contract JSON.
- `TC-CTRL-02`: Returns the complete contract list.
- `TC-CTRL-03`: Filters contracts by hotel name and active date, including out-of-range dates.
- `TC-CTRL-04`: Parses an agent search request and returns an available room result.
- `TC-CTRL-05`: Returns an empty JSON array when no room matches.
- `TC-CTRL-06`: Returns success on valid signup and `400` on duplicate or invalid signup.
- `TC-CTRL-07`: Returns success on valid login and `401` on invalid login.

#### Exception handling: `testing/backend-unit/exception handling/ExceptionHandlingTest.java`

- `TC-EX-01`: Rejects contract creation when the administrator profile is missing.
- `TC-EX-02`: Propagates a database repository failure safely.
- `TC-EX-03`: Returns `400` for malformed contract and search JSON.
- `TC-EX-04`: Returns `400` for an invalid ISO date query parameter.
- `TC-EX-05`: Converts signup `IllegalArgumentException` failures into `400` JSON responses.
- `TC-EX-06`: Converts login `IllegalArgumentException` failures into `401` JSON responses.
- `TC-EX-07`: Handles an empty search JSON object and returns an empty result safely.

#### Database repositories: `testing/DB/DbTest.java`

- `TC-DB-01`: Inserts and retrieves a user and contract administrator.
- `TC-DB-02`: Inserts and retrieves a user and call-center agent.
- `TC-DB-03`: Inserts a contract and verifies its persisted fields.
- `TC-DB-04`: Verifies the contract records its administrator creator.
- `TC-DB-05`: Verifies contract creation cascades into room-type records.
- `TC-DB-06`: Includes contracts at both date boundaries and within the stay period.
- `TC-DB-07`: Excludes expired contracts and contracts ending before checkout.
- `TC-DB-08`: Excludes contracts whose valid-from date is after check-in.
- `TC-DB-09`: Verifies room types are available when room count and adult capacity are sufficient.
- `TC-DB-10`: Verifies insufficient room count or capacity produces no qualifying room outcome.
- `TC-DB-11`: Returns every persisted contract.
- `TC-DB-12`: Retrieves a user with username, password hash, and role.
- `TC-DB-13`: Searches contracts by partial, case-insensitive hotel name.
- `TC-DB-14`: Searches contracts active on one valid date.
- `TC-DB-15`: Searches contracts using both hotel name and valid date.

#### Authentication and authorization: `testing/api/authentication/AuthenticationTest.java`

- `TC-AUTH-01`: Blocks unauthenticated admin and agent API requests.
- `TC-AUTH-02`: Rejects malformed or tampered JWT bearer tokens.
- `TC-AUTH-03`: Rejects expired JWT bearer tokens.
- `TC-AUTH-04`: Authenticates an administrator and allows admin routes while forbidding agent routes.
- `TC-AUTH-05`: Authenticates an agent and allows agent routes while forbidding admin routes.
- `TC-AUTH-06`: Returns `401` for wrong passwords and nonexistent users.
- `TC-AUTH-07`: Rejects missing role-specific signup fields and duplicate usernames.

#### CORS: `testing/api/cors/CorsTest.java`

- `TC-CORS-01`: Allows frontend preflight requests and required CORS headers.
- `TC-CORS-02`: Allows preflight requests on protected routes without a JWT.
- `TC-CORS-03`: Adds allowed-origin headers to actual cross-origin requests.
- `TC-CORS-04`: Rejects requests from unauthorized origins.
- `TC-CORS-05`: Rejects preflight requests using the disallowed `PATCH` method.
- `TC-CORS-06`: Rejects preflight requests using a disallowed custom header.

#### Spring Boot and MySQL integration: `testing/api/spring-db/SpringDbTest.java`

- `TC-E2E-01`: Persists a BCrypt user and the 1:1 role-specific profile through signup.
- `TC-E2E-02`: Creates a contract, administrator foreign key, and cascaded room types.
- `TC-E2E-03`: Lists and filters persisted contracts through the admin API.
- `TC-E2E-04`: Searches an administrator-created contract and calculates marked-up agent pricing.
- `TC-E2E-05`: Excludes expired contracts and reports insufficient inventory as `Unavailable`.
- `TC-E2E-06`: Rejects duplicate username or employee code without changing existing data.
- `TC-E2E-07`: Rejects an agent attempting an administrator contract write and creates no database record.

#### Frontend components: `testing/frontend-unit/components/admin-components.spec.ts`

- `TC-FE-COMP-01`: Displays the logged-in administrator name and contract navigation links.
- `TC-FE-COMP-02`: Fetches contracts and displays hotel, creator, employee code, and room type.
- `TC-FE-COMP-03`: Handles an empty contract response without component errors.

#### Frontend reactive forms: `testing/frontend-unit/pipes/contract-and-search-forms.reactive.spec.ts`

- `TC-FE-FORM-01`: Starts the contract form invalid when hotel and validity dates are empty.
- `TC-FE-FORM-02`: Enforces minimum markup, price, room count, and maximum adult values.
- `TC-FE-FORM-03`: Supports dynamically adding and removing room-type form groups.
- `TC-FE-FORM-04`: Makes the contract form valid when every required field is populated.
- `TC-FE-FORM-05`: Validates check-in date, nights, and the agent room-request array.

#### Frontend display formatting: `testing/frontend-unit/pipes/display-pipes.spec.ts`

- `TC-FE-PIPE-01`: Formats room rates and totals to two decimal places.
- `TC-FE-PIPE-02`: Formats zero and handles null values safely.
- `TC-FE-PIPE-03`: Formats ISO contract dates consistently.

#### Frontend route guards: `testing/frontend-unit/route-guards/auth-and-role.guards.spec.ts`

- `TC-FE-GUARD-01`: Redirects unauthenticated users to `/auth`.
- `TC-FE-GUARD-02`: Allows authenticated users to continue.
- `TC-FE-GUARD-03`: Blocks agents from administrator routes and redirects to the agent dashboard.
- `TC-FE-GUARD-04`: Blocks administrators from agent routes and redirects to the admin dashboard.
- `TC-FE-GUARD-05`: Allows access when the active role matches the route role.

#### Frontend authentication service: `testing/frontend-unit/services/auth-and-jwt.service.spec.ts`

- `TC-FE-SVC-01`: Stores login session data, updates the current user, and navigates to the role dashboard.
- `TC-FE-SVC-02`: Adds an `Authorization: Bearer` header to protected API requests.
- `TC-FE-SVC-03`: Omits authorization headers from unauthenticated requests.
- `TC-FE-SVC-04`: Clears session storage and redirects to `/auth` during logout.

### Overall test status

- **Backend / API / database:** **56 passed, 0 failed**
- **Frontend unit tests:** **20 passed, 0 failed**
- **Frontend production build:** **Passed**
- **Test suites:** **12 suites passed**

## 7. Requirement-oriented test scenarios

| ID | Scenario | Expected result |
|---|---|---|
| TS-01 | Register as contract administrator | Account and administrator profile are created |
| TS-02 | Register as call-center agent | Account and agent profile are created |
| TS-03 | Login with valid credentials | JWT is issued and the correct dashboard opens |
| TS-04 | Login with invalid credentials | Request is rejected with a clear error |
| TS-05 | Create a contract with room types | Contract and room types are persisted |
| TS-06 | Search contracts by hotel/date | Only matching valid contracts are returned |
| TS-07 | Search with enough rooms and capacity | Room type status is `Available` |
| TS-08 | Search with insufficient rooms | Room type status is `Unavailable` |
| TS-09 | Search with excessive adults | Room type status is `Unavailable` |
| TS-10 | Search across an invalid contract period | Contract is not returned |
| TS-11 | Use the wrong role on an endpoint | Request returns `403` |
| TS-12 | Send an unauthenticated request | Request returns `401` |
| TS-13 | Submit invalid form values | Submission is disabled or validation is shown |
| TS-14 | Load an empty contract list | A safe empty state is displayed |

## 8. Entry criteria

- Source code is available and dependencies are installed.
- MySQL is running and the `suntravels` database account is accessible.
- Backend and frontend configuration points to the intended local services.
- Test data can be created or the database is reset to a usable state.

## 9. Exit criteria

Testing is complete when:

- All applicable backend tests pass.
- All applicable frontend tests pass.
- No critical authentication, authorization, persistence, or pricing defect is open.
- Available and unavailable room types are visible with the correct status.

## 10. Test commands

### Backend

Run all backend and integration tests:

```powershell
cd C:\Users\DELL\internship_projects\assignment1\backend
mvn test
```

Run a selected backend suite:

```powershell
mvn -Dtest=BusinessLogicTest test
mvn -Dtest=RESTControllerTest test
mvn -Dtest=AuthenticationTest test
mvn -Dtest=SpringDbTest test
```

### Frontend

Run all frontend tests:

```powershell
cd C:\Users\DELL\internship_projects\assignment1\frontend
npm test -- --watch=false
```

Start the frontend:

```powershell
npm start
```

## 11. Defect reporting

Each defect should include:

- Test case or user story ID.
- Steps to reproduce.
- Expected and actual results.
- Request payload or test data, if relevant.
- HTTP status, browser output, or stack trace.
- Environment and configuration used.
- Severity and screenshots/logs when useful.

