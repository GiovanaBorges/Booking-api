# Booking API

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/SpringBoot-3.x-brightgreen)
![Resilience](https://img.shields.io/badge/Resilience-Resilience4j-purple)
![Spring Security](https://img.shields.io/badge/Security-SpringSecurity-green)
![OAuth2](https://img.shields.io/badge/Auth-OAuth2-black)
![Keycloak](https://img.shields.io/badge/Auth-Keycloak-red)
![OpenAPI](https://img.shields.io/badge/OpenAPI-Documentation-green)
![Swagger](https://img.shields.io/badge/Swagger-UI-brightgreen)
![MySQL](https://img.shields.io/badge/Database-MySQL-blue)
![Redis](https://img.shields.io/badge/Cache-Redis-red)
![Flyway](https://img.shields.io/badge/Migrations-Flyway-red)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-Event--Driven-orange)
![MapStruct](https://img.shields.io/badge/Mapping-MapStruct-blue)
![Tests](https://img.shields.io/badge/Tests-JUnit%20%7C%20Mockito-blue)
![Docker](https://img.shields.io/badge/Container-Docker-blue)
![CI](https://img.shields.io/badge/CI-GitHubActions-blue)
[![codecov](https://codecov.io/gh/GiovanaBorges/Booking-api/branch/main/graph/badge.svg)](https://codecov.io/gh/GiovanaBorges/Booking-api)


Backend service responsible for managing bookings between users and service providers (for example online therapy sessions, consultations, mentoring sessions, etc).

The service exposes REST APIs for creating and managing bookings and publishes domain events to RabbitMQ so other services can react asynchronously, such as the Notification Service responsible for delivering real-time updates to users via WebSocket.

---

# Overview

The Booking API handles the lifecycle of bookings between two actors:

* **User** – the person requesting a session
* **Provider** – the person offering a service

Example scenario:

1. A user schedules a session with a provider
2. The booking is stored by the Booking API
3. A domain event is published to RabbitMQ
4. Other services consume the event
5. The user receives a real-time notification through the frontend

This architecture allows services to remain loosely coupled and react to business events independently.

---

## Running the Project Locally

To run the Booking project locally, you just need Docker and Docker Compose installed. The project is fully containerized, including the backend, database, Redis, RabbitMQ, and Keycloak.

Clone the repository:
git clone https://github.com/GiovanaBorges/Booking-api.git
cd Booking-api
Start the services:
docker-compose up -d

This will spin up all necessary services:

* MySQL for the Booking database
* Redis for caching
* RabbitMQ for messaging
* Keycloak for authentication and authorization
* Booking API backend
* Access the services:
* Booking API – http://localhost:8080
* Keycloak admin console – http://localhost:8080 (username: admin, password: admin123)
* RabbitMQ management – http://localhost:15672

### Stop the services:

```
docker-compose down
```

This setup allows you to run the entire system locally without needing additional installations.

# Architecture Diagram

The system follows a microservices-oriented architecture where the Booking API is responsible for managing bookings and publishing domain events.

Other services can react to these events asynchronously.

```
                  ┌──────────────────────┐
                  │      Frontend        │
                  │  (Web / Mobile App) │
                  └──────────┬───────────┘
                             │
                        REST API
                             │
                     ┌───────▼────────┐
                     │   Booking API  │
                     │  Spring Boot   │
                     └───────┬────────┘
                             │
                       Domain Events
                             │
                       ┌─────▼─────┐
                       │  RabbitMQ │
                       └─────┬─────┘
                             │
                 ┌───────────▼───────────┐
                 │   Notification Service │
                 │  WebSocket Delivery   │
                 └───────────┬───────────┘
                             │
                      Real-time updates
                             │
                        ┌────▼────┐
                        │Frontend │
                        └─────────┘
```

The Booking API publishes events whenever relevant domain changes occur (such as bookings being created, updated, or deleted).

Other services can subscribe to these events without tightly coupling with the Booking API.

---

# Application Architecture

The application follows a **layered architecture**.

### Controllers

Responsible for exposing REST endpoints to the client.

### Services

Contain the core business logic of the application and trigger domain events.

### Repositories

Responsible for persistence and database interaction.

### Events

Represent business actions that occurred inside the system.

### RabbitMQ Producers

Publish events to the message broker so other services can consume them asynchronously.

---

# Event Driven Communication

Instead of tightly coupling services with synchronous REST calls, the system publishes **domain events** through RabbitMQ.

Examples of events emitted by this service:

```
BookingCreatedEvent
BookingUpdatedEvent
BookingDeletedEvent
UsersCreatedEvent
ProviderAvailabilityUpdatedEvent
```

Example flow:

```
User creates booking
      ↓
Booking API stores booking
      ↓
BookingCreatedEvent published
      ↓
RabbitMQ
      ↓
Notification Service consumes event
      ↓
Frontend receives real-time update
```

This pattern improves system scalability, flexibility, and service decoupling.

---

# Design Decisions

## Why RabbitMQ

RabbitMQ was chosen as the message broker because it provides:

* Reliable message delivery
* Flexible routing using exchanges
* Support for asynchronous communication
* Mature ecosystem and strong Spring integration

Using RabbitMQ allows the system to move towards an **event-driven architecture**.

---

## Why Resilience4j

Distributed systems are subject to partial failures.

Resilience4j provides mechanisms to handle failures gracefully:

* **Circuit Breaker** prevents repeated calls to failing services
* **Retry** allows temporary failures to recover automatically
* **Rate Limiter** protects services from overload
* **Bulkhead** isolates failures between components

These mechanisms improve system reliability and prevent cascading failures.

---

## Why Testcontainers

Integration tests should run against **real infrastructure whenever possible**.

Testcontainers allows the project to start real containers during tests.

For example:

* RabbitMQ container is started automatically
* the application connects to it
* tests run against a real message broker

Benefits:

* Tests closer to production behavior
* Less reliance on mocks
* More reliable integration testing

---

## Why MapStruct

DTO mapping can quickly become repetitive and error-prone when implemented manually.

This project uses **MapStruct** to generate mapping code at compile time.

Benefits:

* Type-safe mapping
* No runtime reflection
* Better performance than reflection-based mappers
* Cleaner service layer

Example mapping flow:

```
Entity → Mapper → DTO
DTO → Mapper → Entity
```

This keeps the service layer focused on business logic.

---

## Event Based Notifications

Instead of sending notifications directly from the Booking API, the system publishes events.

The Notification Service consumes those events and delivers them to the frontend through WebSocket.

Benefits:

* Separation of concerns
* Independent scaling of notification service
* Easier to extend with new consumers

---

## 🛠️ Database Migrations (Flyway)

This project uses **Flyway** to handle database schema management.

Currently, the database structure is initialized through a single migration script responsible for creating the schema, tables, and indexes.

---

### 🚀 Initial Migration

The first migration script defines the entire database structure:

```sql
V1__init_schema.sql
```

This script includes:

* Database schema creation
* Table definitions
* Index creation for performance optimization

Flyway automatically executes this script when the application starts for the first time.

---

### ⚙️ How It Works

* Flyway scans the `db/migration` directory
* Detects versioned scripts (`V1`, `V2`, etc.)
* Executes them in order
* Tracks execution in the `flyway_schema_history` table

---

### 📦 Why Use Flyway?

* Ensures consistent database setup across environments
* Eliminates manual database changes
* Provides version control for schema evolution

---

### 💡 Future Improvements

As the project evolves, new migrations will be added instead of modifying the initial script. For example:

```sql
V2__add_new_column.sql
V3__create_new_table.sql
```

This keeps the database history traceable and safe.

---

Flyway acts as the backbone of database consistency, making sure every environment speaks the same structural language.


# Resilience

To increase system reliability, the application uses **Resilience4j**.

Implemented resilience patterns:

* Circuit Breaker
* Retry
* Rate Limiter
* Bulkhead

These patterns help prevent cascading failures and improve the stability of distributed systems.

---

# Security

Authentication and authorization are handled using **Keycloak**.

The API expects **JWT tokens issued by Keycloak** and uses a custom converter to map roles from the token to Spring Security authorities.

---

# Documentation

### 📚 API Documentation (Swagger / OpenAPI)

This project uses **Swagger (OpenAPI)** to provide interactive API documentation.

It allows you to:

* 📌 Explore all available endpoints
* 📌 Test requests directly from your browser
* 📌 View request and response examples
* 📌 Understand parameters, authentication, and status codes

---

### 🚀 How to Access

After starting the application, you can access the documentation at:

```
http://localhost:8081/documentation/swagger-ui/swagger-ui/index.html
```

---

### 🔍 What You Can Do in Swagger UI

* Execute API requests (`GET`, `POST`, `PUT`, `DELETE`)
* Provide parameters and request bodies (JSON)
* View real-time API responses
* Test secured endpoints

---

### 🔐 Authentication

If the API is secured (e.g., JWT / OAuth2), click the **"Authorize"** button in Swagger UI and provide your token in the following format:

```
Bearer YOUR_TOKEN_HERE
```

---

### 🛠️ Technologies Used

* Spring Boot
* Springdoc OpenAPI
* Swagger UI

---

### 📎 Dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

---

### 💡 Note

Make sure the application is running before accessing the Swagger UI.


# Observability

The application exposes metrics through **Spring Boot Actuator** and **Micrometer**.

Metrics are exported in **Prometheus format**, allowing monitoring systems to collect metrics.

Examples of exposed metrics:

* HTTP request latency
* JVM memory usage
* Thread pools
* application metrics

These metrics help monitor the health and performance of the service.

---

# Tech Stack

| Technology      | Purpose                          |
| --------------- | -------------------------------- |
| Java 17         | Main programming language        |
| Spring Boot     | Backend framework                |
| Spring Web      | REST API                         |
| Spring Security | Authentication and authorization |
| Keycloak        | Identity provider                |
| RabbitMQ        | Event messaging                  |
| Redis           | Caching layer                    |
| Resilience4j    | Fault tolerance                  |
| MapStruct       | DTO mapping                      |
| Micrometer      | Application metrics              |
| Prometheus      | Monitoring                       |
| JUnit           | Unit testing                     |
| Mockito         | Mocking framework                |
| Testcontainers  | Integration testing              |
| Docker          | Containerization                 |
| Makefile        | DevOps automation                |
| JaCoCo          | Code coverage                    |
| Swagger/OpenaApi| Documentation                    |
| Flyway          | Migration                        |
---

# Automation

The project includes a **Makefile** to automate common DevOps tasks.

Example commands:

Build Docker image:

```
make build
```

Push image to Docker Hub:

```
make push
```

Run full pipeline:

```
make up
```

The pipeline performs:

1. Build Docker image
2. Authenticate with Docker Hub
3. Push image to registry
4. Configure environment secrets

This simplifies deployment workflows.

---

# Running the Project

### Clone the repository

```
git clone https://github.com/GiovanaBorges/Booking-api.git
```

### Run the application

```
mvn spring-boot:run
```

---

# Running Tests

Run unit tests:

```
mvn clean test
```

Generate coverage report:

```
mvn clean verify
```

JaCoCo report will be generated at:

```
target/site/jacoco/index.html
```

---

# Integration Testing

The project uses **Testcontainers** to run integration tests with real infrastructure.

During tests:

* a RabbitMQ container is started automatically
* the application connects to it
* integration tests run against the real message broker

This ensures the application behaves correctly with real infrastructure instead of relying only on mocks.

---

# Example API Flow

Create booking request:

```
POST /bookings
```

Example result:

```
Booking stored
      ↓
BookingCreatedEvent published
      ↓
RabbitMQ
      ↓
Notification Service consumes event
      ↓
User receives notification
```

---

# Future Improvements

Potential improvements for the system:

* Outbox Pattern for guaranteed event delivery
* Distributed tracing using OpenTelemetry
* Load testing using k6
* Monitoring dashboards using Grafana

---

# Author

**Giovana Borges**

Backend Developer

GitHub
https://github.com/GiovanaBorges
