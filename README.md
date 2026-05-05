# saga-pattern-spring-boot

Demonstration of SAGA Orchestration Design Pattern using Spring Boot and Kafka

## Project Overview

This repository contains a multi-module Spring Boot demo that models an order flow across several services.

Modules:
- `core` - shared DTOs, enums, and common exceptions.
- `orders-service` - accepts order creation requests and stores order state.
- `products-service` - manages products and reservation/cancellation logic.
- `payments-service` - processes payment and calls credit card processor service.
- `credit-card-processor-service` - mock external credit card processor endpoint.

## Tech Stack

- Java 17
- Spring Boot 3.2.x
- Maven (multi-module build)
- PostgreSQL
- Apache Kafka
- Docker Compose (for local infra)

## Prerequisites

- JDK 17+
- Maven 3.9+
- Docker + Docker Compose

## Infrastructure

Start PostgreSQL and Kafka cluster:

```bash
docker compose up -d
```

This starts:
- PostgreSQL on `localhost:5434`
- Kafka brokers exposed on `localhost:9092`, `localhost:9094`, `localhost:9096`

Databases are initialized from `docker/postgres/init-databases.sql`:
- `orders`
- `products`
- `payments`

## Build

From repository root:

```bash
mvn clean install
```

## Run Services

Run each service in a separate terminal:

```bash
mvn -pl orders-service spring-boot:run
mvn -pl products-service spring-boot:run
mvn -pl payments-service spring-boot:run
mvn -pl credit-card-processor-service spring-boot:run
```

Default ports:
- `orders-service`: `8080`
- `products-service`: `8081`
- `payments-service`: `8082`
- `credit-card-processor-service`: `8084`

## API Quick Check

Create product:

```bash
curl -X POST http://localhost:8081/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "price": 1000,
    "quantity": 5
  }'
```

Create order:

```bash
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "11111111-1111-1111-1111-111111111111",
    "productId": "PUT_PRODUCT_ID_HERE",
    "productQuantity": 1
  }'
```

Get order history:

```bash
curl http://localhost:8080/orders/PUT_ORDER_ID_HERE/history
```

## Configuration Notes

- Service configs are in each module under `src/main/resources/application.properties`.
- Database connection can be overridden with environment variables:
  - `POSTGRES_HOST`
  - `POSTGRES_PORT`
  - `POSTGRES_DB`
  - `POSTGRES_USER`
  - `POSTGRES_PASSWORD`
- `payments-service` uses `remote.ccp.url` (default `http://localhost:8084`) to call credit card processor.
