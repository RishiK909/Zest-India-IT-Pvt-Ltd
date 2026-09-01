# Products API

RESTful API for managing Products and their Items, built with Java 17+ and Spring Boot. Supports full CRUD operations, JWT-based authentication with refresh token rotation, and role-based authorization.

## Tech Stack

- Java 17+
- Spring Boot
- Spring Data JPA (Hibernate)
- PostgreSQL
- Spring Security (JWT + Refresh Token)
- JUnit 5 & Mockito
- Swagger / OpenAPI
- Docker & Docker Compose

## Getting Started

### Prerequisites

- Docker Desktop installed and running
- (Optional, for local dev without Docker) Java 21, Maven, PostgreSQL

### Run with Docker

```bash
docker compose up --build
```

This starts:
- `postgres` — PostgreSQL database on port `5432`
- `app` — the Spring Boot application on port `8080`

Once started, the app is available at `http://localhost:8080`.

To stop:

```bash
docker compose down
```

To stop and wipe the database volume:

```bash
docker compose down -v
```

### API Documentation

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Authentication

1. `POST /auth/register` — create a new user
2. `POST /auth/login` — returns an access token and refresh token
3. Use the access token as a Bearer token in the `Authorization` header for protected endpoints
4. `POST /auth/refresh` — rotate an expired access token using the refresh token
5. `POST /auth/logout` — revokes all refresh tokens for the current user

## API Endpoints

| Method | Endpoint                      | Description                          | Access        |
|--------|--------------------------------|---------------------------------------|---------------|
| POST   | `/api/v1/products`            | Create a new product                  | Admin         |
| GET    | `/api/v1/products/{id}`       | Get a product by ID                   | Admin, User   |
| GET    | `/api/v1/products`            | Get all products (paginated)          | Admin, User   |
| PUT    | `/api/v1/products/{id}`       | Update a product                      | Admin         |
| DELETE | `/api/v1/products/{id}`       | Soft-delete a product                 | Admin         |
| GET    | `/api/v1/products/{id}/items` | Get items for a product (paginated)   | Admin, User   |

## Soft Delete

Products and Items are soft-deleted rather than physically removed. A `deletedAt` timestamp is set on delete, and `@SQLRestriction("deleted_at IS NULL")` ensures all read queries automatically exclude deleted records. Deleting a product cascades a soft delete to its items.

## Security & Performance Notes

- **CORS**: configured via `CorsConfigurationSource` in `SecurityConfig` to allow requests from approved frontend origins.
- **HTTPS**: in production, HTTPS is enforced at the reverse-proxy / load-balancer level (e.g. Nginx, AWS ALB); the application itself runs on HTTP internally within the container network.
- **Validation**: request payloads are validated using Jakarta Bean Validation (`@Valid`).
- **Indexing**: `product_id` (foreign key on `item`) is indexed to optimize the items-by-product lookup.

## Git Workflow

Work is organized into feature branches, each merged into `master` once complete:

- `security` — Spring Security, JWT, refresh tokens, CORS
- `deploy` — Docker, Docker Compose, deployment config
- `crud` — Product/Item CRUD endpoints and business logic
- `master` — stable, integrated branch

```bash
git checkout -b feat/product-security   # or deploy / crud
# ... make changes, commit ...
git push origin feat/product-security
# open a PR / merge into master once the branch is ready
```

## Environment Variables

| Variable                          | Description                          | Default (dev)                      |
|-----------------------------------|----------------------------------------|-------------------------------------|
| `SPRING_DATASOURCE_URL`           | JDBC URL for the database             | `jdbc:postgresql://localhost:5432/products_db` |
| `SPRING_DATASOURCE_USERNAME`      | Database username                     | `products_user`                     |
| `SPRING_DATASOURCE_PASSWORD`      | Database password                     | `products_pass`                     |
| `JWT_SECRET`                      | Secret used to sign JWTs              | dev-only placeholder                |
