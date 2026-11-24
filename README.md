# Sun Travels

## Prerequisites

- Java 25
- Maven 3.9+
- Node.js 22+
- npm 10+

## Start the backend

```powershell
cd C:\Users\DELL\internship_projects\assignment1\backend
mvn spring-boot:run
```

The backend starts at `http://localhost:8080` and uses the local MySQL server.
The default connection is `root` with no password:

```properties
jdbc:mysql://localhost:3306/suntravels
```

The database is created automatically when the MySQL account has permission.

## Start the frontend

```powershell
cd C:\Users\DELL\internship_projects\assignment1\frontend
npm install
npm start
```

Open `http://localhost:4200`.

## Run tests

Make sure MySQL is running before running backend or integration tests.

### All backend and integration tests

```powershell
cd C:\Users\DELL\internship_projects\assignment1\backend
mvn test
```

### Individual backend test files

Run these commands from `backend`:

```powershell
mvn -Dtest=BusinessLogicTest test
mvn -Dtest=RESTControllerTest test
mvn -Dtest=ExceptionHandlingTest test
mvn -Dtest=DbTest test
mvn -Dtest=AuthenticationTest test
mvn -Dtest=CorsTest test
mvn -Dtest=SpringDbTest test
```

The Spring DB tests in `SpringDbTest` are the MySQL integration tests.

### All frontend unit tests

```powershell
cd C:\Users\DELL\internship_projects\assignment1\frontend
npm test -- --watch=false
```

### Individual frontend test files

Run these commands from `frontend`:

```powershell
npx ng test --watch=false --include="../testing/frontend-unit/components/admin-components.spec.ts"
npx ng test --watch=false --include="../testing/frontend-unit/pipes/contract-and-search-forms.reactive.spec.ts"
npx ng test --watch=false --include="../testing/frontend-unit/pipes/display-pipes.spec.ts"
npx ng test --watch=false --include="../testing/frontend-unit/route-guards/auth-and-role.guards.spec.ts"
npx ng test --watch=false --include="../testing/frontend-unit/services/auth-and-jwt.service.spec.ts"
```


