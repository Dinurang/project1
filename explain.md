# Sun Travels Project Explained Simply

## What this project does

Sun Travels is a web application for a travel agency that manages hotel contracts and calculates room prices for call-center agents.

The project has two users:

- **Contract Administrator**: loads hotel contracts and room types.
- **Call Center Agent**: searches available contracts and calculates customer prices.

## A simple user journey

### Contract administrator

1. The administrator signs up or logs in.
2. The administrator opens **Load Contract**.
3. The administrator enters:
   - Hotel name
   - Contract start date
   - Contract end date
   - Markup percentage
   - Room types, prices, room counts, and maximum adults
4. The contract is sent to the Spring Boot backend.
5. The backend saves the contract and room types in MySQL.

### Call center agent

1. The agent signs up or logs in.
2. The agent enters:
   - Check-in date
   - Number of nights
   - One or more rooms
   - Number of adults for each room
3. The backend finds contracts that contain the entire stay.
4. The backend checks whether enough rooms exist and whether every room supports its adult count.
5. The frontend displays the hotel, room type, marked-up price, and availability.

## How the price is calculated

The backend uses this calculation:

```text
Marked-up price =
base hotel price
× total adults across all rooms
× number of nights
× (1 + markup percentage / 100)
```

For example:

- Base price: `$100`
- Adults: `3`
- Nights: `2`
- Markup: `15%`

```text
100 × 3 × 2 × 1.15 = $690
```

## Main technologies

### Backend

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- JWT authentication
- MySQL
- Maven
- JUnit and Mockito

### Frontend

- Angular
- TypeScript
- Angular standalone components
- Reactive and template-driven forms
- Angular Router
- HTTP interceptors
- Vitest

## Backend structure

- `controller`: receives browser and API requests.
- `service`: contains authentication and search business logic.
- `repository`: reads and writes database records.
- `model`: represents database entities.
- `dto`: represents request and response data sent through authentication APIs.
- `config`: configures security and JWT filtering.

## Important backend files

- `SunTravelsApplication.java`: starts Spring Boot.
- `AuthController.java`: handles signup and login.
- `AdminContractController.java`: creates and searches contracts.
- `AgentSearchController.java`: receives agent search requests.
- `SearchService.java`: validates contracts, capacity, and prices.
- `AuthService.java`: creates users and role-specific profiles.
- `JwtService.java`: creates and reads JWT tokens.
- `SecurityConfig.java`: controls role access and CORS.
- `application.properties`: contains MySQL connection settings.

## Frontend structure

- `app.routes.ts`: defines the login, admin, and agent pages.
- `app.config.ts`: registers routers and HTTP providers.
- `auth.service.ts`: manages login, logout, JWT storage, and user role.
- `jwt.interceptor.ts`: adds the JWT to protected API requests.
- `auth.guard.ts`: blocks users who are not logged in.
- `role.guard.ts`: prevents a user from entering another role's area.
- `contract-form.component.ts`: loads contracts.
- `contract-list.component.ts`: views contracts.
- `search.component.ts`: accepts agent search requests.
- `search-contract.component.ts`: lets administrators search contracts.

## Database tables

- `app_user`: login username, email, password hash, and role.
- `contract_administrator`: administrator profile linked to a user.
- `call_center_agent`: agent profile linked to a user.
- `contract`: hotel, validity dates, markup, and administrator.
- `room_type`: room name, base price, room count, and maximum adults.

## Authentication

1. A user signs up.
2. The password is encrypted with BCrypt.
3. The login API returns a JWT.
4. Angular stores the JWT in browser local storage.
5. The JWT interceptor sends it in the `Authorization` header.
6. Spring Security checks the token and role.
7. Contract and agent APIs reject users who do not have the correct role.

## Test types

- **Backend unit tests**: test business logic and controllers with Mockito.
- **Database tests**: verify JPA entities and MySQL repositories.
- **API tests**: verify authentication, authorization, CORS, and HTTP responses.
- **Integration tests**: create users and contracts, then run complete workflows.
- **Frontend unit tests**: test components, guards, forms, pipes, auth, and JWT behavior.

## Running the project

1. Start MySQL.
2. Start the backend with `mvn spring-boot:run` in `backend`.
3. Start the frontend with `npm start` in `frontend`.
4. Open `http://127.0.0.1:4200`.

See [README.md](./README.md) for full setup and test commands.
