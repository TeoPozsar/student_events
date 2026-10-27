# Student Events

A web application for discovering and managing student events .

Student Events allows students to explore events and manage their registrations, organizers to manage their own events, and administrators to manage users and oversee events.

## Features

- User registration and login with JWT authentication.
- Role-based access control: `USER`, `ORGANIZER`, and `ADMIN`.
- Event discovery, category filtering, and event details.
- Event creation, editing, and deletion for authorized organizers.
- Event poster upload and display.
- Event registration and cancellation.
- User profile and profile picture management.
- User administration, including role changes, for administrators.
- Automatic event statuses: `UPCOMING`, `IN_PROGRESS`, and `FINISHED`.
- PostgreSQL database with Flyway migrations.

## Tech Stack

**Frontend**
- Angular
- TypeScript
- SCSS

**Backend**
- Java
- Spring Boot
- Spring Security
- JWT authentication
- Maven

**Database and infrastructure**
- PostgreSQL 17
- Docker Compose
- Flyway

## Prerequisites

Install the following before running the application:

- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Java JDK](https://adoptium.net/) compatible with the project's configured Java version
- [Node.js and npm](https://nodejs.org/)

Make sure Docker Desktop is running before starting the database.

## Project Structure

```text
student_events/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   │       └── db/migration/
│   │   └── test/
│   ├── mvnw
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── angular.json
│   └── package.json
├── docker-compose.yml
└── README.md
```

## 1. Clone the Repository

Open PowerShell and run:

```powershell
git clone https://github.com/TeoPozsar/student_events.git
cd student_events
```

## 2. Start the Database

From the project root, run:

```powershell
docker compose up -d postgres
```

The PostgreSQL service is configured to use:

| Setting | Value |
|---|---|
| Database | `student_events` |
| Username | `student_user` |
| Host port | `5433` |
| Container port | `5432` |
| Container name | `student-events-postgres` |

The database is available locally at:

```text
jdbc:postgresql://localhost:5433/student_events
```

The database password is configured through Docker Compose. Check the local development configuration before starting the application.

To check whether the database container is running:

```powershell
docker ps
```

To stop the database:

```powershell
docker compose down
```

Database data is stored in a Docker volume and is normally preserved when the container is stopped.

**Warning:** Do not use `docker compose down -v` unless you intentionally want to remove the database volume and its data.

## 3. Configure the Backend

Open a new PowerShell terminal and navigate to the backend directory:

The backend requires the `JWT_SECRET` environment variable.

For local development, set it in the current PowerShell session:

```powershell
$env:JWT_SECRET = "REPLACE_WITH_A_SECURE_DEVELOPMENT_SECRET"
```

Replace the placeholder with a suitable development secret that satisfies the application's JWT signing requirements.

Start the Spring Boot application:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend is configured to run at:

```text
http://localhost:8081
```

Flyway is configured to apply database migrations from:

```text
backend/src/main/resources/db/migration/
```

Hibernate schema validation checks the database schema during application startup. If startup fails, verify the database connection, credentials, migration files, and required environment variables.

Keep this terminal open while using the application.

## 4. Configure and Start the Frontend

Open another PowerShell terminal and navigate to the frontend directory:

Install dependencies:

```powershell
npm install
```

Start the Angular development server:

```powershell
npm start
```

If the project does not define an `npm start` script, use:

```powershell
npx ng serve
```

Open the application in your browser:

```text
http://localhost:4200
```

The frontend communicates with the backend at `http://localhost:8081`.

## 5. Database Migrations

Flyway manages database schema changes.

Migration scripts are located in:

```text
backend/src/main/resources/db/migration/
```

Migrations are applied automatically when the backend starts, according to the configured Flyway settings.

For a fresh installation, start with an empty database and verify that all migrations execute successfully.


## 6. User Roles

The application supports three roles:

| Role | Main responsibilities |
|---|---|
| `USER` | Browse events, register for events, cancel registrations, and manage their profile. |
| `ORGANIZER` | Browse events and manage events they are authorized to organize. |
| `ADMIN` | Manage users and their roles, and oversee events. |

Access to protected operations is enforced by the backend.

Public registration should not allow users to assign themselves administrative privileges.

## 7. Event Statuses

Event statuses are calculated using the event's start and end date-time.

| Status | Meaning |
|---|---|
| `UPCOMING` | The event has not started yet. |
| `IN_PROGRESS` | The event has started but has not ended. |
| `FINISHED` | The event has reached or passed its end time. |

Finished events are excluded from event discovery, while organizers and administrators can access completed events through their management views.
