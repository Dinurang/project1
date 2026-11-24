# Sun Travels Test Plan

## 1. Purpose

This test plan defines how the Sun Travels application is verified against its
functional requirements, security rules, database behavior, and user workflows.
It is based on the implemented application and the tests in the `testing/`
directory.

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

### 6.1 Backend business-logic tests

Location: `testing/backend-unit/Business Logic/BusinessLogicTest.java`

Verify:

- A room type is `Available` only when enough rooms exist and every requested
  room fits its maximum adult capacity.
- A room type is `Unavailable` when either condition fails.
- Check-out is calculated as check-in date plus number of nights.
- Total price includes base price, total adults, nights, and markup.
- Prices are rounded using `HALF_UP` to two decimal places.
- Incomplete requests return an empty result without querying the repository.

### 6.2 REST controller tests

Location: `testing/backend-unit/REST Controllers/RESTControllerTest.java`

Verify:

- Contract endpoints bind authenticated administrators and return JSON.
- Contract list and search endpoints return expected data.
- Agent search accepts the request shape and returns search results.
- Authentication endpoints return expected success and error responses.
- Malformed or invalid requests are handled with the correct HTTP status.

### 6.3 Exception-handling tests

Location: `testing/backend-unit/exception handling/ExceptionHandlingTest.java`

Verify:

- Missing administrator profiles are rejected safely.
- Repository failures are propagated or translated correctly.
- Malformed JSON returns `400 Bad Request`.
- Invalid date parameters return `400 Bad Request`.
- Invalid signup and login requests return the expected error status.
- Empty search JSON is handled without a null-pointer failure.

### 6.4 Database and repository tests

Location: `testing/DB/DbTest.java`

Verify:

- `user`, `contract_administrator`, and `call_center_agent` records persist.
- Contracts retain their administrator relationship.
- Room types cascade from contract creation.
- Valid contracts include exact boundary and interior stay dates.
- Expired, future, and mid-stay-invalid contracts are excluded.
- Contract listing and hotel/date searches work.
- User lookup returns the stored role and password hash.

### 6.5 Authentication and authorization tests

Location: `testing/api/authentication/AuthenticationTest.java`

Verify:

- Unauthenticated protected requests return `401`.
- Malformed, tampered, and expired JWTs are rejected.
- Administrators can use admin APIs but not agent APIs.
- Agents can use agent APIs but not admin APIs.
- Wrong passwords and unknown users are rejected.
- Missing role-specific signup data and duplicate usernames are rejected.

### 6.6 CORS tests

Location: `testing/api/cors/CorsTest.java`

Verify:

- The configured frontend origin can make preflight and actual requests.
- Protected preflight requests work without a bearer token.
- Unauthorized origins are rejected.
- Disallowed methods and headers are rejected.

### 6.7 End-to-end Spring/MySQL tests

Location: `testing/api/spring-db/SpringDbTest.java`

Verify complete workflows through HTTP, Spring Security, services, JPA, and
MySQL:

- Signup persists the user and role-specific profile.
- Authenticated administrators create contracts and room types.
- Administrators list and filter contracts.
- Agents search persisted contracts and receive marked-up prices.
- Expired contracts are excluded.
- Insufficient room inventory is returned as `Unavailable`.
- Duplicate data is rejected without corrupting existing records.
- Cross-role write attempts return `403` and do not write data.

### 6.8 Frontend unit tests

Locations:

- `testing/frontend-unit/components/admin-components.spec.ts`
- `testing/frontend-unit/pipes/contract-and-search-forms.reactive.spec.ts`
- `testing/frontend-unit/pipes/display-pipes.spec.ts`
- `testing/frontend-unit/route-guards/auth-and-role.guards.spec.ts`
- `testing/frontend-unit/services/auth-and-jwt.service.spec.ts`

Verify:

- Contract and agent search forms enforce required fields and minimum values.
- Room type rows can be added and removed.
- Number and date values are formatted consistently.
- Admin components load contracts and handle empty results.
- Authentication and role guards redirect or allow access correctly.
- Login, logout, and JWT header behavior are correct.

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
- The frontend production build succeeds.
- No critical authentication, authorization, persistence, or pricing defect is
  open.
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

Build the frontend:

```powershell
npm run build
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

