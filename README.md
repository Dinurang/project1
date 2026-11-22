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
