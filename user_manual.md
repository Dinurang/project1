# Sun Travels User Manual

## 1. About the application

Sun Travels helps a travel agency maintain hotel contracts and quote room
prices to customers.

The application has two roles:

- **Contract Administrator:** loads and reviews hotel contracts.
- **Call Center Agent:** searches room types and provides customer quotations.

The application does not create bookings or reserve inventory. It calculates
availability and prices from the saved contract data.

## 2. Starting the application

### Prerequisites

Install and start:

- Java 25
- Maven 3.9+
- Node.js 22+
- npm 10+
- MySQL 8

### Start the backend

Open PowerShell and run:

```powershell
cd C:\Users\DELL\internship_projects\assignment1\backend
mvn spring-boot:run
```

The backend runs at `http://localhost:8080`.

The default database connection is:

```text
jdbc:mysql://localhost:3306/suntravels
Username: root
Password: empty
```

If required, configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` environment
variables before starting the backend.

### Start the frontend

In a second PowerShell window:

```powershell
cd C:\Users\DELL\internship_projects\assignment1\frontend
npm install
npm start
```

Open `http://localhost:4200`.

## 3. Registering an account

1. Open the application. The authentication page is displayed.
2. Select the **Sign Up** tab.
3. Enter a username, email address, and password.
4. Select a role:
   - **Contract Administrator**
   - **Call Center Agent**
5. Complete the role-specific fields:
   - Contract administrator: full name, employee code, and contact number.
   - Call center agent: full name, agent code, and desk extension.
6. Submit the form.
7. After successful registration, use the login form with the new username and
   password.

Usernames and email addresses must be unique. Required role-specific fields
must be supplied.

## 4. Logging in and logging out

### Login

1. Select the **Login** tab.
2. Enter your username and password.
3. Select **Login**.
4. The application opens the dashboard for your role.

Invalid credentials display an error message. Users cannot open protected
features without an authenticated session.

### Logout

Use the logout control in the application navigation. The session token is
removed and the application returns to the authentication page.

## 5. Contract Administrator tasks

### 5.1 Load a hotel contract

1. Log in as a contract administrator.
2. Select **Load Contract**.
3. Enter:
   - Hotel name.
   - Contract start date.
   - Contract end date.
   - Markup percentage.
4. Add at least one room type.
5. For each room type, enter:
   - Room type name.
   - Base price.
   - Number of rooms.
   - Maximum adults per room.
6. Select the save/submit action.
7. Confirm the success message.

The contract date range must be valid. Room prices and room counts cannot be
negative, and maximum adults must be at least one.

### 5.2 View saved contracts

1. Select **View Contracts**.
2. Review each contract's hotel, dates, markup, and room types.
3. Review each room type's base price, room count, and adult capacity.

If no contracts are saved, the page displays an empty state instead of failing.

### 5.3 Search contracts

1. Select **Search Contracts**.
2. Enter a hotel name, an active date, or both.
3. Submit the search.
4. Review the matching contracts.

Date searches return contracts that cover the requested date.

## 6. Call Center Agent tasks

### 6.1 Search rooms

1. Log in as a call-center agent.
2. Select **Search Rooms**.
3. Enter the customer's check-in date.
4. Enter the number of nights. It must be at least one.
5. Enter the number of adults for the first room.
6. Select **+ Add Room** for every additional requested room.
7. Enter the adult count for each room.
8. Select **Search Availabilities**.

The search considers contracts that cover the complete stay. The checkout
boundary is calculated as:

```text
check-in date + number of nights
```

### 6.2 Read search results

The results table shows:

- Hotel name.
- Room type.
- Total marked-up price.
- Availability status.

The result rules are:

- **Available** (green): enough rooms exist for all requested rooms and every
  requested room fits the room type's maximum adult capacity.
- **Unavailable** (red): the room type does not have enough rooms or at least
  one requested room exceeds its adult capacity.

Unavailable room types remain visible so the agent can see all evaluated
options. Their price is shown as `-`; do not quote an unavailable room.

### 6.3 Understand the price

The application calculates each available result as:

```text
base price
× total adults across all requested rooms
× number of nights
× (1 + markup percentage / 100)
```

Example:

```text
Base price:       $100
Total adults:     3
Number of nights: 2
Markup:           15%

$100 × 3 × 2 × 1.15 = $690.00
```

Prices are rounded to two decimal places.

## 7. Navigation and access rules

- Unauthenticated users are redirected to the authentication page.
- Contract administrators can access admin contract pages.
- Call-center agents can access room search.
- A user cannot access the other role's protected pages.
- Directly entering a protected URL does not bypass authentication or role
  checks.

## 8. Common messages and solutions

| Situation | Solution |
|---|---|
| Login fails | Check the username and password, then try again |
| Signup fails | Check required fields and make sure username/email are unique |
| No search results | Check dates, nights, room count, and adult counts |
| All room types are unavailable | Reduce the requested rooms/adults or ask about another contract |
| Backend connection error | Confirm MySQL and the Spring Boot backend are running |
| Page cannot reach the API | Confirm the frontend uses `http://localhost:8080` and the backend is available |
| Contract cannot be saved | Check date order, numeric limits, and at least one room type |

## 9. Stopping the application

In each PowerShell window running the backend or frontend, press `Ctrl+C`.

## 10. Useful verification commands

Run backend tests:

```powershell
cd C:\Users\DELL\internship_projects\assignment1\backend
mvn test
```

Run frontend tests:

```powershell
cd C:\Users\DELL\internship_projects\assignment1\frontend
npm test -- --watch=false
```


