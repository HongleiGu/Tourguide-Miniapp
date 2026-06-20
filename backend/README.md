# Backend — Spring Boot 3.5 (Java 21)

The API service for the tour-guide platform.

- Maven, Spring Boot 3.5.x, Java 21 (generated via Spring Initializr)
- Layered packages: `api` / `service` / `domain` / `repository` / `config` / `common`
- MySQL 8 + Flyway (MIN-12), Redis (MIN-13), OpenAPI/Swagger (MIN-14)
- Standard response envelope (`common.ApiResponse`) + global exception handling

## Run

```bash
# from backend/
./mvnw spring-boot:run          # uses the 'dev' profile by default
./mvnw clean test               # build + tests
```

Health: `GET /actuator/health` · Sample: `GET /api/ping`

## Configuration & profiles (MIN-11)

Config lives in `src/main/resources/`:

- `application.yml` — base config; secrets read from env vars (see root `.env.example`)
- `application-dev.yml` — default profile; safe defaults so it boots **without** real secrets
- `application-prod.yml` — strict; required secrets must come from the environment (missing
  values fail fast via `AppProperties` validation)

Select a profile:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run        # or set in the environment
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod    # Maven plugin flag
```

Secrets (`JWT_SECRET`, `WX_*`, `WXPAY_*`, `DB_*`, `REDIS_*`) are injected from the
environment — never commit real values. Copy `.env.example` to `.env` and fill it in.
Typed binding for the `app.*` tree is in `config/AppProperties`.

## Data infrastructure

- **MySQL 8** (MIN-12) — schema via Flyway (`src/main/resources/db/migration/`), JPA `ddl-auto=validate`.
- **Redis** (MIN-13) — Lettuce/`RedisTemplate` + Spring cache for hot reads; **Redisson** for the
  distributed lock. Helpers live in `common/redis/`:
  - `DistributedLock` — Redisson-backed; makes "check seats then claim" atomic (group-buy, MIN-4).
  - `DelayedTaskQueue` — ZSet-based delayed queue; powers group-buy timeout auto-void (MIN-4).
  - `RedisKeys` — the single source of key conventions: `lock:*`, `cache:*`, `delay:*`.

Run both via Docker for local dev:

```bash
docker compose up -d mysql redis     # from repo root
```

