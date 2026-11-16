# Supermarket — Microservices Final Project

An online supermarket built as a microservices system: catalog and stock, orders, customers,
search, product images, authentication, an API gateway, Kafka events, Docker Compose and Kubernetes.

## Stack

- Java 21, Spring Boot 4.1.2, Maven multi-module (root `pom.xml` is an aggregator)
- Spring Cloud Gateway (WebFlux)
- PostgreSQL 15 + Spring Data JPA (Hibernate 7)
- Apache Kafka 3.9 (KRaft mode, no Zookeeper)
- Elasticsearch 9.4.8 + Spring Data Elasticsearch
- S3 (AWS SDK v2) on LocalStack 4.12
- JWT (jjwt) in an HttpOnly cookie
- JUnit 5, Mockito, MockMvc, H2, JaCoCo
- Docker Compose, Kubernetes (Docker Desktop)

## Services

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8000 | Single entry point, routing, JWT check |
| product-service | 8080 | Products, categories, stock, product images (through image-service) |
| order-service | 8081 | Orders; decreases stock in product-service |
| customer-service | 8082 | Customer profiles |
| search-service | 8083 | Product search (Elasticsearch), fed by Kafka |
| image-service | 8084 | Image upload and delete in S3, presigned URLs |
| auth-service | 8085 | Users, register / login / logout, JWT issuing |

## Project structure

```
supermarket/
├── pom.xml                  (aggregator)
├── docker-compose.yml       (entire stack)
├── .env.example             (copy to .env)
├── api-gateway/
├── auth-service/
├── product-service/
├── order-service/
├── customer-service/
├── search-service/
├── image-service/
├── infra/
│   ├── postgres/init/       (creates a database + user per service)
│   └── localstack/init/     (creates the S3 bucket)
├── k8s/                     (Kubernetes manifests)
└── postman/                 (Postman collections)
```

## Run with Docker Compose

1. Create the secrets file from the example and fill in the values:
   ```
   cp .env.example .env
   ```
   `.env` is not committed. `JWT_SECRET` must be at least 32 characters.
2. Start everything:
   ```
   docker compose up -d --build
   ```
3. The API is available through the gateway: `http://localhost:8000`.
   The business services are not exposed outside the Docker network.

Clean start (removes all data):
```
docker compose down -v
```

### Run a service from the IDE

Start only the infrastructure, then run the services from the IDE
(each service defaults to `localhost` addresses):
```
docker compose up -d postgres kafka elasticsearch localstack
```

## Run the tests

```
mvn -pl product-service verify
mvn -pl order-service verify
```

Each test run creates a JaCoCo coverage report in `<service>/target/site/jacoco/index.html`.

Tests exist for **product-service** and **order-service**: a unit test (Mockito), a controller
slice test (`@WebMvcTest`) and an integration test (`@SpringBootTest` + H2). Integration tests use
`src/test/resources/application.properties` (H2 in memory, Kafka topic creation disabled), so they run
without Docker.

## Deploy to Kubernetes

Tested on the Kubernetes cluster of Docker Desktop (kubeadm, single node), which uses local images.

1. Build the images:
   ```
   docker compose build
   ```
2. Deploy:
   ```
   kubectl apply -f k8s/
   ```
3. Check the pods:
   ```
   kubectl get pods -n supermarket
   ```
4. The gateway is exposed on NodePort `http://localhost:30000`.

Remove everything:
```
kubectl delete namespace supermarket
```

Deployed in Kubernetes (as listed in the project document): namespace, Postgres, secrets,
auth-service, product-service, order-service (**2 replicas**) and api-gateway.
Kafka, Elasticsearch, LocalStack, customer-service, search-service and image-service run only in Docker Compose.
In Kubernetes the three services share one Postgres database and user, and the secrets in
`k8s/20-secrets.yaml` are demo values.

### Flow 5 — self-healing

```
kubectl get pods -n supermarket
kubectl delete pod <one-order-service-pod-name> -n supermarket
```
Send "Create order" from the Kubernetes Postman collection while the pod is recreated — the second
replica answers. A readiness probe makes sure the new pod gets traffic only when it is ready.
(Deleting by label, `-l app=order-service`, removes both replicas at once.)

## Postman

- `postman/supermarket-flows.postman_collection.json` — Flows 1–4 through the gateway
  (Docker Compose). The setup folder creates its own category and products, so it runs on any database.
  Set `adminPassword` and `userPassword` to the seed passwords from `.env`.
- `postman/supermarket-k8s.postman_collection.json` — Kubernetes (port 30000) and Flow 5.

Every request has a short description of what it tests.

## API overview

All requests go through the gateway.

| Method | Path | Access |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login`, `/api/auth/logout` | public |
| GET | `/api/auth/me` | logged in |
| GET | `/api/products?categoryId=`, `/api/products/{id}`, `/api/categories` | public |
| POST / PUT / DELETE | `/api/products/**`, `/api/categories/**` | admin |
| POST | `/api/products/{id}/image` (multipart, field `file`) | admin |
| DELETE | `/api/products/{id}/image` | admin |
| GET | `/api/search/products?q=&categoryId=` | public |
| GET | `/api/images/products/{productId}/{fileName}` | public (302 to a presigned URL) |
| POST | `/api/orders` | logged in |
| GET | `/api/orders/me` | logged in |
| GET | `/api/orders/{id}` | owner or admin |
| GET | `/api/orders`, PATCH `/api/orders/{id}/status` | admin |
| GET / PUT / DELETE | `/api/customers/me` | logged in |
| GET / PUT / DELETE | `/api/customers`, `/api/customers/{userId}` | admin |

Seed users (created on auth-service startup, passwords from `.env`): `admin` (ADMIN, USER), `user`, `user2`.

## Design decisions

### Data
- One Postgres container, a **separate database and user per service** (created by an init script that
  runs only on an empty volume). Services never share tables; cross-service consistency is handled in code.
- Schema by Hibernate `ddl-auto=update`.
- Secrets only in `.env` (not committed) and `.env.example`; no default values for secrets.
- DTOs are records, validation only on request DTOs, entities never leave a service, separate mapper classes.
- `open-in-view=false`; mapping runs inside `@Transactional` service methods; `@EntityGraph` against N+1.
- Products: `BigDecimal` price (2 decimals), unique SKU, non-negative stock constraint, required category.
- Uniqueness is checked in the service (409) with a DB unique constraint as a safety net.

### Errors
- Errors follow RFC 9457 `ProblemDetail`; successful responses return the data without a wrapper.
- 201 responses include a `Location` header, DELETE returns 204.

### Orders and stock
- Order items keep a snapshot of the product name and price at purchase; only `productId` refers to product-service.
- One batch call from order-service to product-service decreases stock, all-or-nothing:
  duplicates are merged, missing stock rejects the whole order with 409 and the list of unavailable products.
- Stock rows are locked with a pessimistic write lock (`SELECT ... FOR UPDATE`) in id order to avoid deadlocks.
- The HTTP call to product-service runs outside any DB transaction; RestClient timeouts (connect 2s, read 5s);
  product-service 4xx are passed through, 5xx/unreachable become 503.
- Order status can be changed by an admin to any value (no transition rules).
- `POST /api/orders` body is `{"items": [...]}` (the document shows a bare array).

### Security
- auth-service issues a JWT (claims: username, user id, roles), HS256, stored in an HttpOnly, SameSite=Strict
  cookie `ACCESS_TOKEN`. Stateless, CSRF disabled (SameSite cookie).
- Gateway `JwtGatewayFilter`: public routes are auth, and GET on products, categories, search and images;
  anything else needs a valid token (401). The internal stock endpoint and non-GET image requests are blocked with 404.
- Each service has its own `JwtAuthFilter` + `@EnableMethodSecurity` + `@PreAuthorize`
  (defense in depth). `AccessDeniedException` → 403 (401 for anonymous users).
- Token relay: order-service and product-service forward the user's cookie when they call another service.

### Kafka
- Single broker in KRaft mode; topics `product-events` and `order-events` created on startup.
- Events are published **after commit** (`@TransactionalEventListener`), keyed by entity id, full snapshot in each event.
- search-service consumers: `ErrorHandlingDeserializer`, 3 retries, then a dead-letter topic;
  `auto-offset-reset=earliest` so a new index is rebuilt from the topic history.

### Search
- Index `products`; free text on name (boosted ×3) and description with fuzziness; exact category filter.
- `soldCount` is updated from order events.

### Images
- Admin uploads through product-service, which forwards the file to image-service (only JPEG/PNG/WEBP, up to 5MB).
- S3 key `products/{productId}/{uuid}.{ext}`; the bucket is private.
- The product stores a **stable path** (`/api/images/...`); every GET returns a 302 to a fresh presigned URL (15 minutes).
- Deleting a product image (or the product itself) first clears the image path in the DB, then deletes the
  file from S3 (best effort: if image-service fails, the file stays and a warning is logged).
  Uploading a new image does not delete the previous file.
- The same code works with real S3: leave `S3_ENDPOINT` empty and set real credentials.

## Known limitations

- If saving an order fails after stock was decreased, the stock is not returned (no compensation / saga).
- Kafka publishing after commit can lose an event if Kafka is down at that moment (no outbox); failures are only logged.
- Consumers are at-least-once: a duplicate order event would count a sale twice in search.
- Renaming a category does not update already-indexed products.
- Internal endpoints are hidden only at the gateway; services trust the forwarded token.
- Asking for another customer's order returns 403 (as required), so order ids can be enumerated.
- Gateway errors for an unknown route or an unavailable service use the default WebFlux format, not ProblemDetail.
- Replaced images stay in S3, and a failed S3 delete leaves an orphan file. LocalStack keeps data in memory only,
  so restarting it removes the uploaded images.
- Without Kafka in Kubernetes, every write that publishes an event waits up to 3 seconds before the send fails.
- Tests and coverage reports exist only for product-service and order-service.
- Security filter code is duplicated in each service (no shared library); field injection is used.
