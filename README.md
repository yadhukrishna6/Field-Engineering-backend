# Field Engineering - Standalone Quarkus REST API Backend

A high-performance, cloud-native REST API backend for the Field Engineering Tablet Application, built with **Java 17**, **Quarkus 3.8.3**, **Hibernate ORM with Panache**, **PostgreSQL**, **Flyway**, and **AWS S3 / MinIO Object Storage**.

---

## 🚀 Quick Start

### 1. Requirements
- **Java 17** (e.g. Amazon Corretto 17, Eclipse Temurin 17)
- **Maven 3.8.0+**
- *(Optional)* **Docker** for PostgreSQL & MinIO object storage

### 2. Run in Development Mode (with Live Reload & In-Memory H2 DB)
`ash
mvn quarkus:dev
`
- **REST API Base URL:** http://localhost:8081
- **Interactive Swagger UI:** http://localhost:8081/q/swagger-ui
- **Quarkus Dev UI:** http://localhost:8081/q/dev
- **OpenAPI Schema:** http://localhost:8081/q/openapi

---

## 🗄️ Local Infrastructure (PostgreSQL & MinIO)

To run standalone local PostgreSQL and MinIO S3 object storage containers:

`ash
docker compose up -d
`

- **PostgreSQL 16:** localhost:5432 (ield_engineering_db / user: ield_admin / pass: ield_secure_password_2026)
- **MinIO S3 API:** http://localhost:9000 (minioadmin / minioadmin)
- **MinIO Web Console:** http://localhost:9001

---

## 📦 Project Structure

`
field-engineering-backend/
├── docker-compose.yml          # Local PostgreSQL & MinIO S3 containers
├── pom.xml                     # Quarkus dependencies & build configuration
├── .gitignore                  # Git ignore rules
└── src/
    └── main/
        ├── java/com/fieldengineering/
        │   ├── domain/         # Hibernate Panache Entities (Projects, Drawings, Issues, Inspections, etc.)
        │   ├── resource/       # JAX-RS REST Endpoints (Auth, Sync, Drawing, Markup, Equipment, etc.)
        │   └── service/        # S3 / MinIO Object Storage Service
        └── resources/
            ├── application.properties    # Quarkus configuration
            └── db/migration/             # Flyway SQL schema migrations
`

---

## 🔌 Core API Endpoints

| Resource | Path | Description |
| :--- | :--- | :--- |
| **Authentication** | POST /api/auth/login | Authenticate user & issue JWT |
| **Two-Way Delta Sync** | POST /api/sync/batch | Offline queue batch sync & conflict resolution |
| **Projects** | GET/POST /api/projects | Field projects management |
| **Drawings** | GET/POST /api/drawings | Blueprints, vector tiles, PDF metadata |
| **Revisions** | GET/POST /api/revisions | Blueprint revisions and comparison |
| **Markups** | GET/POST /api/markups | Vector drawing annotations & callouts |
| **Measurements** | GET/POST /api/measurements | Calibrated CAD measurements |
| **Issues / Punch List** | GET/POST /api/issues | Deficiency tracking & status updates |
| **Photos** | GET/POST /api/photos | Site photo evidence & S3 storage |
| **Inspections** | GET/POST /api/inspections | Checklists & sign-offs |
| **Equipment** | GET/POST /api/equipment | Heavy equipment tracking & QR tags |
| **Reports** | GET/POST /api/reports | Daily report generation |
| **Voice Notes** | GET/POST /api/voice-notes | Field voice memos |
| **Audit Trail** | GET /api/audit-logs | Immutable audit log trail |

---

## 🏗️ Build & Package

`ash
# Package standard Uber-JAR / Fast-JAR
mvn clean package -DskipTests

# Build Native Binary (requires GraalVM)
mvn clean package -Dnative -DskipTests
`
