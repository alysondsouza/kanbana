# PHASE 2 — Application

---

# Chapter 9 — Kanbana REST API (Onion Architecture)

## Checklist

### 9.1 — Package structure
> Set up the Onion layers before writing any code.
- [x] Create packages: domain, application, infrastructure, api under com.kanbana
- [x] Keep KanbanaApplication.java at the root (com.kanbana)
- [x] Move AppController.java into the api package
- [x] Keep application.properties (no change needed)

### 9.2 — Dependencies
> Add everything we need to pom.xml in one shot.
- [x] Add spring-boot-starter-data-jpa
- [x] Add postgresql driver (runtime scope)
- [x] Add spring-boot-starter-flyway + flyway-database-postgresql
- [x] Add spring-boot-starter-validation
- [x] Add H2 (test scope)
- [x] Add springdoc-openapi-starter-webmvc-ui (Swagger)

### 9.3 — Database migrations (Flyway)
> Define the schema in versioned SQL files. Flyway runs these on startup.
- [x] Create src/main/resources/db/migration/
- [x] V0__create_schema.sql — creates kanbana schema
- [x] V1__create_users.sql
- [x] V2__create_boards.sql (FK → users)
- [x] V3__create_columns.sql (FK → boards)
- [x] V4__create_cards.sql (FK → board_columns)
- [x] V5__remove_owner_fk.sql — drops boards_owner_id_fkey (no users until Phase 3)
- [x] Run app and verify "Successfully applied 5 migrations to schema kanbana" in logs

### 9.4 — Domain layer
> Pure Java. No Spring, no JPA. This layer defines what the app IS.
- [x] Create domain entity: User.java (POJO — no annotations)
- [x] Create domain entity: Board.java
- [x] Create domain entity: BoardColumn.java
- [x] Create domain entity: Card.java
- [x] Create repository interfaces (ports): BoardRepository, ColumnRepository, CardRepository

### 9.5 — Infrastructure layer
> This layer knows about the database so the domain does not have to.
- [x] Create JPA entity: UserEntity.java (@Entity, @Table(schema="kanbana"), @Id)
- [x] Create JPA entities: BoardEntity, BoardColumnEntity, CardEntity
- [x] Create Spring Data repositories implementing domain interfaces
- [x] Create mappers: domain object <-> JPA entity (ID set to null in toEntity())

### 9.6 — Application layer (services)
> Use cases and business logic live here. Services call domain repositories.
- [x] Create request/response DTOs (CreateBoardRequestDTO, BoardResponseDTO, etc.)
- [x] Add @NotBlank, @Size validation to request DTOs
- [x] Create BoardService (createBoard, getBoard, listBoards, deleteBoard)
- [x] Create ColumnService (addColumn, getColumnsByBoard, deleteColumn)
- [x] Create CardService (createCard, updateCard, moveCard, deleteCard)

### 9.7 — API layer (controllers)
> Controllers translate HTTP <-> service calls. Nothing else.
- [x] Create BoardController — GET/POST /api/v1/boards, GET/DELETE /api/v1/boards/{id}
- [x] Create ColumnController — POST/GET /boards/{boardId}/columns, DELETE /columns/{id}
- [x] Create CardController — POST/GET /columns/{columnId}/cards, PATCH/DELETE /cards/{id}, PATCH /cards/{id}/move
- [x] Create GlobalExceptionHandler (@RestControllerAdvice)
- [x] Define ErrorResponseDTO record (status, message, timestamp)

### 9.8 — Configuration & Running the Application
> 12-Factor: all config from environment variables, no hardcoded values.
- [x] Configure datasource in application.properties using ${DB_HOST}, ${DB_PORT}, ${DB_NAME} env vars
- [x] Set spring.jpa.hibernate.ddl-auto=none (Flyway owns the schema)
- [x] Set spring.flyway.default-schema=kanbana
- [x] Create application-test.properties overriding datasource to H2 in-memory

### 9.9 — Tests
> Unit tests for logic, integration tests for the full stack against H2.
- [x] Unit test: BoardService (mock repository, test happy path + edge cases)
- [x] Unit test: GlobalExceptionHandler error response shapes
- [x] Integration test: BoardController (POST → 201, GET → 200, missing → 404)
- [x] Integration test: validation (empty title → 400 with message)
- [x] mvn test — all green

### 9.10 — Deploy
> Update Ansible to inject DB config, build, deploy, and verify end-to-end.
- [x] Update Ansible appserver role: pass DB env vars to the Docker container
- [x] Encrypt DB credentials with Ansible Vault
- [x] mvn clean package && ansible-playbook app-server.yml
- [x] Smoke test: POST a board via HTTP, verify row in Postgres on db-server
- [x] git tag v2.0-rest-api && git push

---

## Functional Requirements & Business Rules

### Boards
- **must** — a user can create, read, and delete a board
- **must** — a board has a title and belongs to one owner
- **rule** — deleting a board cascades to all its columns and cards
- **must not** — a board title cannot be empty
- **out of scope** — board sharing between users (requires auth, Phase 3)

### Columns
- **must** — a user can add, rename, reorder, and delete columns on a board
- **rule** — a column must always belong to exactly one board
- **rule** — deleting a column cascades to all its cards
- **must not** — a column cannot exist without a parent board

### Cards
- **must** — a user can create, update, move, and delete a card
- **must** — a card can be moved to a different column on the same board
- **rule** — a card must always belong to exactly one column
- **rule** — cards have a position — order within a column is preserved
- **must not** — a card cannot be moved to a column on a different board

### API behaviour
- **must** — all errors return a consistent JSON body (status, message, timestamp)
- **must** — invalid input returns 400 with a validation message
- **must** — missing resource returns 404
- **must not** — stack traces are never exposed to the client

### Out of scope (Phase 2)
User authentication, role-based access, board sharing, real-time updates,
file attachments, labels/tags on cards.

---

## 9.0 — Onion Architecture

### The diagram

```
        ┌─────────────────────────────┐
        │            API              │  ← outermost: controllers, HTTP
        │   ┌─────────────────────┐   │
        │   │     Application     │   │  ← services, use cases, DTOs
        │   │   ┌─────────────┐   │   │
        │   │   │    Infra    │   │   │  ← JPA, repositories, DB
        │   │   │  ┌───────┐  │   │   │
        │   │   │  │Domain │  │   │   │  ← innermost: pure Java, no deps
        │   │   │  └───────┘  │   │   │
        │   │   └─────────────┘   │   │
        │   └─────────────────────┘   │
        └─────────────────────────────┘
```

### The one rule

Dependencies only point inward. Outer layers import inner layers — never the reverse.

```
API  →  Application  →  Infrastructure  →  Domain
                                           (no imports at all)
```

### Each layer — what lives there and how they connect

| Layer | Package | What lives here | May import |
|---|---|---|---|
| Domain | `domain/` | Domain entities, repository interfaces | Nothing |
| Infrastructure | `infrastructure/` | JPA entities, Spring Data repositories | Domain |
| Application | `application/` | Services, DTOs | Domain, Infrastructure |
| API | `api/` | Controllers | Application, Domain |

**Domain** — `domain/model/` and `domain/repository/`
The core of the application. Pure Java, zero framework imports.
- **Domain entities** (`Board`, `Card`) — plain Java objects representing business concepts.
  No `@Entity`, no annotations. Just fields and methods that express business meaning.
- **Repository interfaces** (`BoardRepository`) — define what the app needs from
  persistence. Method signatures only, no implementation. These are contracts.

**Infrastructure** — `infrastructure/persistence/`
The only layer that knows about the database.
- **JPA entities** (`BoardEntity`) — Java classes annotated with `@Entity`, `@Table`,
  `@Column`. Hibernate maps these to database rows. They mirror the SQL schema.
  JPA only knows how to save `@Entity` classes — it cannot save a plain `Board` object.
- **Spring Data repositories** (`BoardJpaRepository`) — interfaces that extend
  `JpaRepository`. Spring generates the SQL implementation at runtime.

**Application** — `application/service/` and `application/dto/`
Orchestrates the use cases. Knows what needs to happen, not how it is stored or served.
- **Services** (`BoardService`) — contain business logic. Call domain repository interfaces.
- **DTOs** (`CreateBoardRequestDTO`, `BoardResponseDTO`) — plain objects shaped for the API.
  Never expose domain entities or JPA entities to the outside world.

**API** — `api/`
The front door. Speaks HTTP, nothing else.
- **Controllers** (`BoardController`) — receive HTTP requests, call services, return
  HTTP responses wrapped in `ResponseEntity`. Work with DTOs only.

### How a request flows through all four layers

Reading guide: each block shows one layer. The label in brackets is the object
being passed at that moment. The method shown is called BY that layer, not received.

```
CLIENT sends:
  POST /api/v1/boards  {"title": "My Board"}

┌─────────────────────────────────────────────────────────────────────┐
│ API — BoardController                                                │
│  Receives JSON, deserialises it into CreateBoardRequestDTO (DTO)     │
│  Calls → boardService.createBoard(request)                           │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ [CreateBoardRequestDTO]
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Application — BoardService                                           │
│  Builds a Board (domain object) from the DTO fields                  │
│  id and createdAt generated here: UUID.randomUUID(), Instant.now()   │
│  Calls → boardRepository.save(board)                                 │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ [Board — domain object]
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Infrastructure — BoardRepositoryAdapter                              │
│  Converts Board → BoardEntity  (mapper — ID set to null)             │
│  Hibernate runs: INSERT INTO kanbana.boards ...                      │
│  Postgres writes the row, returns it with generated UUID             │
│  Converts BoardEntity → Board  (mapper)                              │
│  BoardEntity never leaves this layer                                 │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ [Board — domain object]
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Application — BoardService                                           │
│  Converts Board → BoardResponseDTO (DTO)                             │
│  Returns BoardResponseDTO to controller                              │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ [BoardResponseDTO]
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│ API — BoardController                                                │
│  Wraps BoardResponseDTO in ResponseEntity with status 201            │
│  Spring serialises to JSON automatically                             │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
CLIENT receives:
  201 Created  {"id": "abc-123", "title": "My Board"}
```

Read it like this:
The client sends JSON → the controller turns it into a `CreateBoardRequestDTO` →
the service turns that into a `Board` domain object → infrastructure converts it to
`BoardEntity` (ID null so JPA generates it) → Postgres writes the row → comes back
as `Board` → the service wraps it in a `BoardResponseDTO` → the controller wraps
that in `ResponseEntity` → client gets JSON back.

Each layer touches exactly one type of object. That is the discipline.

---

## 9.1 — Package Structure

### Package structure

**Before:**
```
com.kanbana/
├── KanbanaApplication.java
└── AppController.java
```

**After:**
```
com.kanbana/
├── KanbanaApplication.java        ← stays here
├── api/
│   └── AppController.java         ← moved here
├── application/
│   ├── service/                   ← empty for now
│   └── dto/                       ← empty for now
├── infrastructure/
│   └── persistence/               ← empty for now
└── domain/
    ├── model/                     ← empty for now
    └── repository/                ← empty for now
```

### Steps

**Step 1 — Create the packages**

```bash
mkdir -p src/main/java/com/kanbana/{domain/{model,repository},application/{service,dto},infrastructure/persistence,api}
```

**Step 2 — Keep KanbanaApplication.java at the root**

Do not move it. `@SpringBootApplication` scans from its own package downward.
If it were inside a sub-package it would not see sibling packages.

**Step 3 — Move AppController.java to the api package**

Move the file to `src/main/java/com/kanbana/api/`
Update the package declaration at the top:

```java
// Before
package com.kanbana;

// After
package com.kanbana.api;
```

**Step 4 — application.properties stays as-is**

No change needed. We will add datasource, JPA, and Flyway config in step 9.8.

### Verify
```bash
mvn spring-boot:run
curl http://localhost:8080/hello
# → Hello World from embedded Tomcat!
```

Nothing functionally changes in 9.1. We are only reorganising.
If the app breaks, check the package declaration in AppController.java first.

### Running locally vs on the server

```
Option A — Local (WSL)
─────────────────────────────────────────
WSL
 └── Java 21 (installed directly)
      └── mvn spring-boot:run
           └── starts embedded Tomcat on port 8080
                └── curl localhost:8080/hello ✅

Option B — Server (Ansible)
──────────────────────────────────────────
app-server VM
 └── Docker
      └── eclipse-temurin:21 container
           └── java -jar kanbana.jar
                └── starts embedded Tomcat on port 8080
                     └── curl <vm-ip>:8080/hello ✅
```

Both run the same JAR and the same embedded Tomcat.
The difference is only where Java lives.

- Local: Java is in WSL. `mvn spring-boot:run` compiles and runs in one step.
- Server: Java is in a Docker container. Ansible copies the built JAR and starts it.

⚠️ Once datasource config is added in 9.8, running locally will try to connect to Postgres.
It will fail unless you pass env vars pointing at db-server, or have Postgres running locally.

---

## 9.2 — Dependencies

Add inside the `<dependencies>` block in pom.xml. No versions needed —
the Spring Boot parent POM manages them automatically (except springdoc).

### What each dependency does

1. **spring-boot-starter-data-jpa** — brings in Hibernate (translates Java objects to SQL),
   Spring Data JPA (gives you `save()`, `findById()` etc. for free), and HikariCP
   (manages a pool of database connections).

2. **postgresql** `(runtime scope)` — the JDBC driver, the cable between Java and Postgres.
   Runtime scope because you never import it in your code — your code talks to JDBC
   interfaces, the driver handles the actual Postgres communication underneath.

3. **spring-boot-starter-flyway** — the Flyway migration engine wired correctly for
   Spring Boot 4 auto-configuration. Use this starter, not `flyway-core` directly.

4. **flyway-database-postgresql** — required separately. Flyway 11 splits database-specific
   support into separate modules. Without this, Flyway throws "Unsupported Database: PostgreSQL 17".

5. **spring-boot-starter-validation** — brings in Hibernate Validator. Lets you put
   `@NotBlank`, `@Size`, `@Email` on DTO fields. Spring rejects invalid requests
   with a 400 before they reach your service code.

6. **h2** `(test scope)` — an in-memory database used only during tests. Never ships
   in the production JAR. Flyway runs your real SQL migrations against it so tests
   use the real schema without needing Postgres running.

7. **springdoc-openapi-starter-webmvc-ui** `(version 2.8.6)` — generates Swagger UI
   automatically from your controllers. Version must be specified — not managed by
   Spring Boot parent POM.

### Verify

```bash
mvn dependency:tree | grep -E "jpa|postgresql|flyway|validation|h2|springdoc"
```

---

## 9.3 — Database Migrations (Flyway)

### How Flyway works

On every startup Flyway:
1. Looks in `src/main/resources/db/migration/`
2. Checks its own `flyway_schema_history` table in the database
3. Runs any SQL files not yet applied — in version order

Filenames are a contract: `V1__create_users.sql`
- `V1` = version (must be unique and sequential)
- `__` = double underscore (required separator)
- rest = description (free text)

Once a file has been applied, Flyway checksums it.
If you modify an applied file, Flyway will refuse to start.
Migrations are write-once. To fix a mistake, add a new migration.

### Foreign keys and ON DELETE CASCADE

A `REFERENCES` is a foreign key — it tells Postgres this column must contain
a value that exists in another table. Postgres enforces this at insert time.

`ON DELETE CASCADE` tells Postgres what to do when the parent row is deleted.
Without it, deleting a parent with existing children throws an error.
With it, the delete cascades automatically down the tree:

```
Delete a user
 └── CASCADE → all their boards are deleted
      └── CASCADE → all board_columns are deleted
           └── CASCADE → all cards are deleted
```

### Migrations

- **V0__create_schema.sql** — creates the `kanbana` schema
- **V1__create_users.sql** — users table, the root of all foreign keys
- **V2__create_boards.sql** — boards table, FK → users
- **V3__create_columns.sql** — board_columns table, FK → boards
- **V4__create_cards.sql** — cards table, FK → board_columns
- **V5__remove_owner_fk.sql** — drops `boards_owner_id_fkey`

V5 exists because `boards.owner_id` references `users`, but no users exist until
authentication is implemented in Phase 3. Without dropping this FK, every board
creation fails with a constraint violation. The `owner_id` column is kept — the
FK will be restored in Phase 3 when auth is implemented.

```
Database structure:
  db:     kanbana
  schema: kanbana  ← not public
```
Named `board_columns` not `columns` — COLUMN is a reserved SQL keyword.

### Verify

```bash
# Check schema and tables exist
docker exec -it postgres psql -U kanbana -d kanbana -c "\dn"
docker exec -it postgres psql -U kanbana -d kanbana -c "\dt kanbana.*"

# Check Flyway applied all migrations
docker exec -it postgres psql -U kanbana -d kanbana \
  -c "SELECT version, description, success FROM kanbana.flyway_schema_history;"
```

Expected output: 6 rows (schema creation + 5 migrations), all `success = t`.

---

## 9.4 — Domain Layer

The innermost layer. Pure Java — no Spring, no JPA, no framework imports.
This layer defines what the application IS, not how it stores or exposes anything.

### Two types of classes

**Domain entities** (`domain/model/`) — represent business concepts as plain Java objects.
Fields only hold data that matters to the business, not to the database.
References between entities use IDs (UUID), not object references — avoids JPA coupling.

**Repository interfaces** (`domain/repository/`) — define what the application needs
from persistence, without caring how it is done. These are ports.
No annotations. No implementation. Just method signatures.

### Why two classes per concept (Board vs BoardEntity)?

- `Board.java` — what a board *means* to the application (business object)
- `BoardEntity.java` — how a board is *stored* in the database (infrastructure detail)

They look similar now but diverge over time. A database change should never
force a domain change. Keeping them separate enforces that boundary.

### The @Repository / @Service rule

`@Repository` and `@Service` are Spring annotations — they mark a class for
automatic injection. They belong only in infrastructure and application layers.
The domain layer must never import Spring.

### Dependency Inversion (the D in SOLID)

`BoardService` declares it needs a `BoardRepository` (the domain interface).
Spring finds `BoardRepositoryAdapter` (infrastructure, annotated `@Repository`,
implements the interface) and injects it. The service never knows which
implementation it got — only the contract matters.

```
BoardService → BoardRepository (interface)
                     ↑
               BoardRepositoryAdapter (@Repository, infrastructure)
```

### Files created

```
domain/model/       Board.java, User.java, BoardColumn.java, Card.java
domain/repository/  BoardRepository.java, ColumnRepository.java, CardRepository.java
```

### Verify
```bash
mvn compile   # should produce BUILD SUCCESS with zero errors
```

---

## 9.5 — Infrastructure Layer

The only layer that knows about the database.
All files go in `src/main/java/com/kanbana/infrastructure/persistence/`.

### Files created

```
JPA entities:    UserEntity, BoardEntity, BoardColumnEntity, CardEntity
Mappers:         BoardMapper, BoardColumnMapper, CardMapper
JPA repos:       BoardJpaRepository, ColumnJpaRepository, CardJpaRepository
Adapters:        BoardRepositoryAdapter, ColumnRepositoryAdapter, CardRepositoryAdapter
```

### Four types of classes per concept

```
BoardEntity              — JPA entity. @Entity, @Table(name="boards", schema="kanbana").
                           Hibernate maps this to a database row.
                           Requires a protected no-arg constructor for Hibernate.
                           Never leaves infrastructure.

BoardJpaRepository       — Spring Data interface (JpaRepository<BoardEntity, UUID>)
                           Spring generates SQL at runtime. Body is empty for basic CRUD.
                           Custom queries declared as method names (findByBoardId, etc.)

BoardRepositoryAdapter   — @Repository class, implements domain BoardRepository interface.
                           Delegates to BoardJpaRepository, runs mappers.
                           This is what Spring injects into BoardService.

BoardMapper              — Utility class, static methods only, not instantiable.
                           toDomain(BoardEntity) → Board
                           toEntity(Board)       → BoardEntity  (ID set to null — see below)
```

### How the four classes interact

```
  BoardService
  (application layer)
       │
       │ depends on (injected by Spring)
       ▼
  BoardRepository                  ← domain interface, method signatures only
       ▲
       │ implements
       │
  **BoardRepositoryAdapter**       ← [1] @Repository, this is the real implementation
       │
       ├── calls **BoardMapper**.toEntity(board)         [2] Board → BoardEntity (ID=null)
       │
       ├── calls **BoardJpaRepository**.save(entity)     [3] Hibernate runs INSERT
       │         (Spring Data — SQL generated at runtime)
       │
       ├── Postgres writes the row, returns **BoardEntity** with generated UUID  [4]
       │
       ├── calls **BoardMapper**.toDomain(entity)        [2] BoardEntity → Board
       │
       └── returns Board to BoardService
```

The four infrastructure classes:
- **[1] BoardRepositoryAdapter** — the coordinator, implements the domain interface
- **[2] BoardMapper** — the translator, converts between Board and BoardEntity
- **[3] BoardJpaRepository** — the SQL executor, talks to Postgres via Hibernate
- **[4] BoardEntity** — the database object, the only type Hibernate understands

### Why the Adapter exists (type mismatch problem)

`JpaRepository.save()` takes a `BoardEntity` and returns a `BoardEntity`.
`BoardRepository.save()` takes a `Board` and returns a `Board`.
The types are different — one interface cannot satisfy both signatures.

The Adapter solves this: speaks `Board` to the outside world,
`BoardEntity` to JPA internally. The mapper handles the conversion.

### Why JPA entities need a protected no-arg constructor

Hibernate creates entity objects via reflection — calls the no-arg constructor,
creates an empty object, then fills fields from the database row.
`protected` prevents your own code from calling `new BoardEntity()` accidentally.

### Spring Data derived query methods

```
findByBoardId  →  SELECT * FROM board_columns WHERE board_id = ?
findByColumnId →  SELECT * FROM cards WHERE column_id = ?
```
No `@Query` annotation, no SQL written by hand.

### Why ID is set to null in toEntity()

Spring Data's `save()` checks if the entity is new by checking if the ID is null.
Non-null ID → Spring Data runs UPDATE → fails on a non-existent row →
`ObjectOptimisticLockingFailureException`.

Null ID → INSERT → Postgres generates the UUID via `DEFAULT gen_random_uuid()` →
returned entity contains the real UUID → mapped back to the domain object.

### Verify
```bash
mvn compile   # 39 source files — BUILD SUCCESS
```

---

## 9.6 — Application Layer

Orchestrates use cases. Contains business logic, DTOs, and services.
No HTTP knowledge here — that belongs in the API layer.
No database knowledge here — that belongs in infrastructure.

### Files created

**DTOs** — `application/dto/`

| File | Direction | Purpose |
|---|---|---|
| `CreateBoardRequestDTO` | incoming | title for a new board |
| `BoardResponseDTO` | outgoing | id, title, ownerId, createdAt |
| `CreateColumnRequestDTO` | incoming | title for a new column |
| `ColumnResponseDTO` | outgoing | id, title, position, boardId |
| `CreateCardRequestDTO` | incoming | title + description for a new card |
| `UpdateCardRequestDTO` | incoming | optional title and/or description (PATCH) |
| `MoveCardRequestDTO` | incoming | targetColumnId to move a card to |
| `CardResponseDTO` | outgoing | id, title, description, position, columnId |

**Services** — `application/service/`

| File | Methods |
|---|---|
| `BoardService` | createBoard, getBoardById, getAllBoards, deleteBoard |
| `ColumnService` | addColumn, getColumnsByBoard, deleteColumn |
| `CardService` | createCard, getCardsByColumn, updateCard, moveCard, deleteCard |
| `EntityNotFoundException` | thrown when a resource is not found → mapped to HTTP 404 in 9.7 |

### DTOs — what they are and why they exist

A DTO (Data Transfer Object) is a plain object whose only job is to carry
data across a boundary — in this case between HTTP and the application layer.

- **Request DTOs** — carry data arriving from HTTP into the service.
  Annotated with `@NotBlank`, `@Size` etc. so Spring validates input before it
  reaches service code.
- **Response DTOs** — carry data from the service back to the controller,
  which serialises them to JSON.

Each response DTO has a static `from()` factory method:
```java
public static BoardResponseDTO from(Board board) { ... }
```

### Services — what they do

Services are annotated with `@Service` and use constructor injection.
Each service method follows the same pattern:
1. Validate inputs / check parent resources exist
2. Build or retrieve domain objects
3. Call repository
4. Return a response DTO

**BoardService** — `id` and `createdAt` are generated here:
```java
new Board(UUID.randomUUID(), request.getTitle(), UUID.randomUUID(), Instant.now())
```
The placeholder `ownerId` (`UUID.randomUUID()`) will be replaced with the
real authenticated user's ID in Phase 3.

**EntityNotFoundException** — thrown by any service when a resource is not found.
`GlobalExceptionHandler` catches it and returns HTTP 404. Services never deal with
HTTP status codes directly.

### Jackson and constructors

Jackson (the library Spring uses to convert JSON → Java object) works by creating
an empty object first, then setting each field one by one using the setter methods.
Without the no-arg constructor, Jackson cannot create the object and throws an error.

The parameterised constructor is just a convenience for your own code — for example,
in tests you can write `new CreateCardRequestDTO("My card", "description")` instead
of creating an empty object and calling setters manually. Jackson never uses it.

### Partial updates (PATCH)

`UpdateCardRequestDTO` has no `@NotBlank` on title — it is a PATCH, not a PUT.
You send only the fields you want to change. The rest stay null.

```json
{ "title": "New title" }
{ "description": "New description" }
{ "title": "New title", "description": "New description" }
```

In `CardService.updateCard()` null fields are skipped:

```java
if (request.getTitle() != null)       card.setTitle(request.getTitle());
if (request.getDescription() != null) card.setDescription(request.getDescription());
```

### How the application layer connects to the rest of the project

```
HTTP request
     ↓
BoardController (api)           calls boardService.createBoard(request)
     ↓
BoardService (application)      validates, builds Board domain object
     ↓                          calls boardRepository.save(board)
BoardRepository (domain)        interface
     ↓
BoardRepositoryAdapter (infra)  maps to BoardEntity, calls JPA, maps back
     ↓
Postgres
     ↑
BoardRepositoryAdapter          returns Board domain object
     ↑
BoardService                    calls BoardResponseDTO.from(board), returns DTO
     ↑
BoardController                 wraps in ResponseEntity, Spring serialises to JSON
     ↑
HTTP 201 response
```

### Verify
```bash
mvn compile
```

---

## 9.7 — API Layer

The outermost layer. Speaks HTTP — nothing else.
Controllers receive requests, call services, return responses.
No business logic, no database knowledge.
All files go in `src/main/java/com/kanbana/api/`.

### Files created

**Controllers**

| File | Endpoints |
|---|---|
| `BoardController` | POST /api/v1/boards, GET /api/v1/boards, GET /api/v1/boards/{id}, DELETE /api/v1/boards/{id} |
| `ColumnController` | POST /api/v1/boards/{boardId}/columns, GET /api/v1/boards/{boardId}/columns, DELETE /api/v1/columns/{id} |
| `CardController` | POST /api/v1/columns/{columnId}/cards, GET /api/v1/columns/{columnId}/cards, PATCH /api/v1/cards/{id}, PATCH /api/v1/cards/{id}/move, DELETE /api/v1/cards/{id} |

**Error handling**

| File | Layer | Why |
|---|---|---|
| `EntityNotFoundException` | `application/service/` | thrown by services — application concern |
| `GlobalExceptionHandler` | `api/` | maps exceptions to HTTP status codes — API concern |
| `ErrorResponseDTO` | `api/` | HTTP response shape — API concern |

### Why error handling is split across two layers

`ErrorResponseDTO` is shaped purely for HTTP — it has a `status` field, a `message`,
and a `timestamp`. It belongs in `api/`.

`GlobalExceptionHandler` maps `EntityNotFoundException` (application layer) to HTTP 404.
That mapping is an API concern — the application layer throws exceptions without knowing
anything about HTTP. The API layer decides what HTTP status those exceptions become.

### HTTP status codes

| Code | Meaning | Where it comes from |
|---|---|---|
| 200 OK | Successful GET or PATCH | `ResponseEntity.ok(dto)` in controller |
| 201 Created | Successl POST | `ResponseEntity.status(HttpStatus.CREATED).body(dto)` |
| 204 No Content | Successful DELETE | `ResponseEntity.noContent().build()` |
| 400 Bad Request | Validation failed | `GlobalExceptionHandler` catches `MethodArgumentNotValidException` |
| 404 Not Found | Resource does not exist | `GlobalExceptionHandler` catches `EntityNotFoundException` |
| 500 Internal Error | Unhandled exception | `GlobalExceptionHandler` catch-all |
| 403 Forbidden | Not authorised | Spring Security — Phase 3, not yet ilemented |

### Key annotations

**`@RestController`** — every method return value is written directly to the HTTP response body as JSON.

**`@RequestMapping("/api/v1/boards")`** — base URL prefix for all methods in the class.

**`@GetMapping`, `@PostMapping`, `@PatchMapping`, `@DeleteMapping`** — map a method to an HTTP verb.

**`@PathVariable`** — extracts a value from the URL: `/boards/{id}` → `UUID id`.

**`@RequestBody`** — deserialises JSON into a DTO object (Jackson).

**`@Valid`** — trlidation on the DTO. If any constraint fails, Spring throws
`MethodArgumentNotValidException` before the method runs. `GlobalExceptionHandler` catches it → 400.

**`@RestControllerAdvice`** — marks `GlobalExceptionHandler` as a global interceptor.

### Validation flow

```
POST /api/v1/boards  {"title": ""}
         ↓
  BoardController     @Valid triggers Bean Validation on CreateBoardRequestDTO
         ↓
  @NotBlank fails     Spring throws MethodArgumentNotValidException
         ↓
  GlobalExcepr  catches it, returns:
         ↓
  400 Bad Request  {"status": 400, "message": "title: Board title must not be blank", "timestamp": "..."}
```

The service is never called. Invalid data never reaches business logic.

### Swagger (OpenAPI)

Swagger automatically generates interactive API documentation from your controllers.

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

No Java conf needed — Spring Boot auto-detects springdoc on the classpath.

Open in Windows browser after starting the app:
```
http://localhost:8080/swagger-ui.html
```

### Verify
```bash
mvn compile
```

---

## 9.8 — Configuration & Running the Application

12-Factor App rule: all config comes from environment variables — never hardcoded.
The `${VAR:default}` syntax means: use the env var if set, fall back to the default.

### application.properties

```properties
# ── Datasource ────────â────────────────────────────────────────────
# Connects via PgBouncer (port 6432) — never directly to Postgres (5432)
# ${VAR:default} — uses env var if set, falls back to default for local dev
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:6432}/${DB_NAME:kanbana}
spring.datasource.username=${DB_USER:kanbana}
spring.datasource.password=${DB_PASSWORD:secret}
spring.datasource.driver-class-nesql.Driver

# ── Flyway ────────────────────────────────────────────────────────────────────
# Runs migrations on startup — creates all tables before Hibernate sees the schema
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.default-schema=kanbana

# ── JPA / Hibernate ──────────────────â────────────────────────────────
# none — Flyway owns the schema entirely. This is correct long-term, not a workaround.
# Flyway guarantees schema correctness via checksums — Hibernate validation is redundant
# and causes startup ordering failures on empty databases.
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.default_schema=kanbana

# ── DEBUG ───â───────────────────────────────────────────────────────────────
# Uncomment these when you get a 500 or unexpected behaviour — check the app logs
# spring.jpa.show-sql=true
# logging.level.com.kanbana=DEBUG
# logging.level.org.springframework.web=DEBUG
```

### application-test.properties

```properties
# Test profile — overrides application.properties for mvn test
# H2 in-memdatabase — no Postgres needed, Flyway runs migrations against H2

# ── Datasource ────────────────────────────────────────────────────────────────
# MODE=PostgreSQL makes H2 behave like Postgres (same SQL syntax)
# DB_CLOSE_DELAY=-1 keeps the database alive for the full test run
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
spring.datasource.drives-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# ── JPA / Hibernate ───────────────────────────────────────────────────────────
# none — Flyway handles all schema creation, Hibernate makes no changes
spring.jpa.hibernate.ddl-auto=none

# ── Flyway ──────────────────────────────────â────────────────────────────
# Runs the real migration files against H2 — same schema as production
spring.flyway.enabled=true
```

---

### Debugging

Before running the app, verify the infrastructure is healthy.
Work through these checks in order — network → Docker → PgBouncer → Postgres → Flyway.

#### Maven

```bash
# Check Flyway and autoconfigure versions resolved correctly
mvn dependency:tree | grep -i "flyway\|boot-autoconfigure"#### Network — from WSL

```bash
# Check db-server VM IP (changes on every restart)
multipass list

# Ping the VM
ping <db-server-ip>

# Check WSL network routes
ip route
```

#### Docker — inside db-server VM

```bash
# Both postgres and pgbouncer must show Up
docker ps

# Start containers if stopped
docker start postgres
docker start pgbouncer

# Verify both containers are on kanbana-net
docker network inspect kanbana-net

# Reconnect a container if missing from the network
docker network connect kanbnet pgbouncer
```

#### PgBouncer — inside db-server VM

```bash
# Must show 0.0.0.0:6432 — if it shows 5432, LISTEN_PORT was not set
docker exec -it pgbouncer netstat -tlnp

# Check logs for auth errors
docker logs pgbouncer

# Test PgBouncer connection
docker exec -it pgbouncer psql -h pgbouncer -p 6432 -U kanbana -d kanbana -c "SELECT 1"
```

#### Postgres — inside db-server VM

```bash
# Check logs
docker logs postgres

# Test direct connection
docker exec -it postgres psql -U kanbana -d kanbana -ECT 1"

# List databases
docker exec -it postgres psql -U kanbana -d kanbana -c "\l"

# List schemas (should show kanbana, not just public)
docker exec -it postgres psql -U kanbana -d kanbana -c "\dn"

# List tables in kanbana schema
docker exec -it postgres psql -U kanbana -d kanbana -c "\dt kanbana.*"

# Check nothing landed in public schema by mistake
docker exec -it postgres psql -U kanbana -d kanbana -c "\dt"
```

#### Flyway — inside db-server VM

```bash
# Check migration history — all 6 rows shoshow success = t
docker exec -it postgres psql -U kanbana -d kanbana \
  -c "SELECT version, description, success FROM kanbana.flyway_schema_history;"
```

---

### Running locally

```bash
# Get current db-server IP
multipass list

# Run the app
DB_HOST=<db-server-ip> DB_PASSWORD=<password> mvn spring-boot:run
```

If port 8080 is already in use:
```bash
lsof -i :8080        # find what process is holding it
fuser -k 8080/tcp    # kill it
```

### Verify — startup logs

```
Successfully validated 6 migraons           ← Flyway checksums OK
Schema "kanbana" is up to date                ← no new migrations needed
Tomcat started on port 8080                   ← app is up
```

On first run against a fresh database:
```
Creating schema "kanbana"
Successfully applied 5 migrations to schema "kanbana"
```

### API smoke test

---

```bash
# Create a board
curl -X POST http://localhost:8080/api/v1/boards \
  -H "Content-Type: application/json" \
  -d '{"title": "My First Board"}'
# Expected: 201 Created + JSONid, title, ownerId, createdAt

# Verify the row landed in Postgres
docker exec -it postgres psql -U kanbana -d kanbana \
  -c "SELECT * FROM kanbana.boards;"
```

If you get a 500, uncomment the DEBUG lines in `application.properties` and restart.
The full exception will appear in the terminal where `mvn spring-boot:run` is running.

### Browser

WSL shares `localhost` with Windows — open directly in any Windows browser:

```
http://localhost:8080/hello   ← Hello World smoke test
```

---

### 9.8.1 —er

Swagger automatically generates interactive API documentation from your controllers.
No extra Java config needed — Spring Boot auto-detects `springdoc` on the classpath.

**What was needed:**

1. Add to `pom.xml` (version must be explicit — not managed by Spring Boot parent):
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

2. Open in browser:
```
http://localhost:8080/swagger-ui.
```

All endpoints are grouped by controller. Expand any endpoint, fill in parameters,
and click Execute — it sends a real HTTP request to your running app.

**Every request is real** — Swagger hits your actual running app and writes to the
actual database. It is not a simulation.

**The two response codes shown** — Swagger documents all possible responses for an
endpoint. The one that actually fired (e.g. `201 Created`) is the real result.
The others (e.g. `200 OK`) are just schema documentation shohat the endpoint
could return. Ignore everything except the one that matches your actual response.

**Reading the curl Swagger generates:**
```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/boards' \
  -H 'accept: */*' \          ← tells the server you accept any response format
  -H 'Content-Type: application/json' \
  -d '{"title": "Aly"}'       ← -d = data = the request body
```

**What Swagger is good for:**
- Quickly testing endpoints while developing
- Seeing all endpoints in one place withir expected inputs and outputs
- Sharing API documentation — teammates can read and test without writing code

**What Swagger is not good for:**
- Saving and reusing requests
- Running multiple requests in sequence
- Automated or authenticated testing (limitations with auth headers)

**Happy path test sequence — verified end to end:**

1. `POST /api/v1/boards` → 201, board written to `kanbana.boards`
2. `POST /api/v1/boards/{id}/columns` → 201, column linked to board, position = 0
3. `POST /api/v1/cid}/cards` → 201, card linked to column
4. `GET /api/v1/boards/{id}` → 200, board retrieved
5. `DELETE /api/v1/boards/{id}` → 204, board and all children deleted via CASCADE

```
HTTP → Controller → Service → Repository → Postgres → back up
```

⚠️ When using `{id}` parameters, paste the exact UUID from the previous response
including all dashes. Swagger may pre-fill the field with placeholder text.

---

### 9.8.2 — Postman

Postman is a tool for manually testing your API.
Open it, tyRL, choose a verb (GET/POST/PATCH/DELETE), add a body, hit Send.
Think of it as a browser that can send any HTTP request — not just GET.

**The key difference from Swagger** — Postman saves your requests in a **collection**.
You build them once and re-run them anytime, without the app needing to be running
to see the documentation.

**Getting started:**
1. Download and install from `https://www.postman.com/downloads/`
2. Create a free account (or skip login)
3. Click **New → Collection** → name it "
4. Click **New → HTTP Request**
5. Set method to `POST`, URL to `http://localhost:8080/api/v1/boards`
6. Click **Body → raw → JSON**, paste `{"title": "My Board"}`
7. Click **Send** → you get the response back
8. Click **Save** — the request is now stored in your collection

**The real power — chaining requests with variables:**
```
1. POST /api/v1/boards           → save returned id as {{boardId}}
2. POST /api/v1/boards/{{boardId}}/columns  → use it automatically
3. POST /api/v1/columns/{{columnId}}/cards  → chain further
```

Postman can extract values from one response and pass them to the next request
automatically using environment variables — no manual copy-pasting of UUIDs.

**When to use Postman over Swagger:**
- When you want to save and replay a sequence of requests
- When testing authenticated endpoints (auth headers are easier to manage)
- When you want to share a test collection with teammates
- Phase 3 onwards — Swagger has limitations with JWT auth headers

**For now** â is sufficient. Postman becomes essential in Phase 3.

---

## 9.9 — Tests

> Unit tests for logic, integration tests for the full stack against H2.

---

### Key concepts

**What is H2?**

H2 is an in-memory database written in Java. It starts when the JVM starts and disappears when the JVM stops. No Docker, no Postgres, no network — it lives entirely in RAM.

`application-test.properties` tells Spring to use H2 instead of Postgres during tests:
```properties
spring.datasource.url=jdbc:h2:mem:testdb;MPostgreSQL
```
`MODE=PostgreSQL` makes H2 understand most Postgres SQL syntax. However some Postgres-specific features do not work in H2 — discovered during this project:
- `gen_random_uuid()` → replace with no default, generate UUID in Java instead
- `PRIMARY KEY DEFAULT <fn>()` → H2 does not support inline defaults after `PRIMARY KEY`
- Named FK constraints (e.g. `boards_owner_id_fkey`) → H2 auto-names constraints differently
- `DROP CONSTRAINT <named>` → fails if the name doesn't match H2's autd name

Flyway runs the real migration files against H2 on startup — same schema as production.
Each test run gets a fresh empty database.

**How does the integration test write to the DB?**

`MockMvc` simulates an HTTP request → hits the real `DispatcherServlet` → calls the real `BoardController` → calls the real `BoardService` → calls the real `BoardRepositoryAdapter` → runs a real SQL INSERT into H2. The full stack runs, just with H2 instead of Postgres and no real HTTP port open.

**What is 
Hamcrest is a matcher library for readable assertions. MockMvc's `jsonPath()` integrates with it:
```java
.andExpect(jsonPath("$.message").value(containsString("title")))
```
`containsString("title")` checks the message contains the word "title" without caring about the rest. This makes tests less brittle — the exact wording can change as long as the field name still appears.

**What is a bean?**

A bean is any object that Spring creates and manages. When you annotate a class with `@Service`, `@Repositor, or `@RestController`, Spring creates one instance at startup and stores it in the **application context** — essentially a map of beans.

`@Autowired` asks Spring to look up the matching bean and inject it. This is Dependency Injection.

Spring Boot 4 no longer registers `com.fasterxml.jackson.databind.ObjectMapper` as a bean (replaced by Jackson 3's `tools.jackson.databind.json.JsonMapper`). That's why the integration test uses `new ObjectMapper()` directly instead of `@Autowired`.

---

### Two categors

**Unit tests** — test one class in isolation. No Spring context, no database, no network.
Fast (milliseconds). Use JUnit 5 + Mockito to replace real dependencies with fakes.

**Integration tests** — start a real Spring context with H2 in-memory database.
Flyway runs the real migration files against H2 on startup — same schema as production.
Slower (seconds), but test the full request-to-response path.

---

### Files to create

```
src/test/java/com/kanbana/
    application/service/BoardServiceTest    ← unit test
    api/GlobalExceptionHandlerTest.java           ← unit test
    api/BoardControllerTest.java                  ← integration test
```

---

### Unit test: BoardServiceTest

**Class under test:** `BoardService`
**Dependencies mocked:** `BoardRepository` (via `@Mock`)
**Framework:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)

**Walkthrough:**
- `@ExtendWith(MockitoExtension.class)` — plugs Mockito into JUnit 5. Without this, `@Mock` is ignored.
- `@Mock` — creates a fRepository`. Every method returns null/empty by default. No database is touched.
- `@InjectMocks` — creates a real `BoardService` and injects the mock into it via constructor injection.
- `when(...).thenAnswer(...)` — stubs the fake to return a specific value when called.
- `verify(..., never())` — asserts a method was never called. Used to confirm guard clauses work.

**Common mistake:** forgetting `@ExtendWith` — your mocks silently stay null and you get `NullPointerException`.

Cases:
- `createBopy path → `boardRepository.save()` is called, returned DTO has correct title
- `getBoardById` found → returns DTO
- `getBoardById` not found → throws `EntityNotFoundException`
- `deleteBoard` happy path → `boardRepository.deleteById()` is called
- `deleteBoard` not found → throws `EntityNotFoundException`

---

### Unit test: GlobalExceptionHandlerTest

**Class under test:** `GlobalExceptionHandler`
**Framework:** `MockMvc` in standalone mode (no Spring context — just the handler wired up)

**Whnt from BoardServiceTest:**
`BoardServiceTest` tested pure Java logic — no HTTP at all. Here we need HTTP because `GlobalExceptionHandler` only activates when an exception bubbles up through Spring MVC's request pipeline. `MockMvcBuilders.standaloneSetup()` builds a fake DispatcherServlet with only what you give it — no DB, no full context. A `FakeController` inner class triggers specific exceptions on demand.

Cases:
- `EntityNotFoundException` thrown → response is 404, body has `status=404` and corressage`
- `MethodArgumentNotValidException` triggered → response is 400, body has `status=400`
- The 500 test asserts the message is a safe generic string, not the real exception message — explicit OWASP security check.

---

### Integration test: BoardControllerTest

**What makes this different from the previous two tests:**

`@SpringBootTest` starts the entire application — all beans, all layers, Flyway runs migrations against H2, everything wires together. When `mockMvc.perform(post(...))` runs, thest travels through the real `DispatcherServlet` → `BoardController` → `BoardService` → `BoardRepositoryAdapter` → H2.

Spring Boot 4 removed `@AutoConfigureMockMvc` and `@WebMvcTest` from the test-autoconfigure jar. Instead, MockMvc is built manually from the `WebApplicationContext`:
```java
MockMvcBuilders.webAppContextSetup(context).build()
```

`ObjectMapper` is no longer a Spring bean in Boot 4 — instantiate directly: `new ObjectMapper()`.

Cases:
- `POST /api/v1/boards` with valid body → 2y contains `id` and `title`
- `GET /api/v1/boards/{id}` with existing id → 200
- `GET /api/v1/boards/{id}` with random UUID → 404
- `POST /api/v1/boards` with blank title → 400, error body contains `"title"`

---

### H2 compatibility fixes applied to migrations

H2 does not support all Postgres SQL. These changes were required to make migrations run on both:

| Original (Postgres only) | Fixed (works on both) |
|---|---|
| `UUID PRIMARY KEY DEFAULT gen_random_uuid()` | `UUID PRIMARY KEY` — UUID genn Java |
| `NOW()` | `CURRENT_TIMESTAMP` |
| Named FK constraint in V2 | Removed — FK enforced at application layer |
| `V5__remove_owner_fk.sql` drops named constraint | Emptied — FK was removed from V2 |

---

### Spring Boot 4 breaking changes encountered

| Change | Impact | Fix |
|---|---|---|
| `@AutoConfigureMockMvc` removed | Integration test won't compile | Use `MockMvcBuilders.webAppContextSetup()` |
| `ObjectMapper` no longer a bean | `@Autowired ObjectMapper` fails | Use `new ObjectMapper()`ectly |
| Jackson 3 (`tools.jackson.*`) is primary | `com.fasterxml.*` ObjectMapper not registered | Same fix as above |
| `spring-boot-starter-webmvc-test` doesn't exist | Wrong dependency in pom.xml | Replace with `spring-boot-starter-test` |

---

### Running the tests

Run a single test class:
```bash
mvn test -pl . -Dtest=BoardServiceTest
mvn test -pl . -Dtest=GlobalExceptionHandlerTest
mvn clean test -pl . -Dtest=BoardControllerTest   # clean required after migration file changes
```

Force download test dependencies if compilation fails:
```bash
mvn dependency:resolve -Dscope=test
```

Run all tests:
```bash
mvn test
```

⚠️ `mvn clean test` (all tests) may fail if `KanbanaApplicationTests` tries to start with Postgres env vars missing. Fix: annotate `KanbanaApplicationTests` with `@ActiveProfiles("test")` so it uses H2.

---

### Note on scope

`ColumnServiceTest` and `CardServiceTest` follow the exact same pattern as `BoardServiceTest`.
Write those yourself after `BoardServiceTest` is green — tucture is identical.

---

### Checklist

- [x] `BoardServiceTest` — unit, all 5 cases green
- [x] `GlobalExceptionHandlerTest` — unit, 404 and 500 cases green
- [x] `BoardControllerTest` — integration, all 4 cases green
- [x] `KanbanaApplicationTests` annotated with `@ActiveProfiles("test")`
- [x] `mvn test` — all green, no failures

---

## 9.10 — Deploy

> Build the JAR, write production config via Ansible template, deploy to app-server, verify end-to-end.

---

### How Spring Boot finds `appliperties`

Spring Boot checks config locations in this order — later entries win:

```
1. classpath:/                    ← inside the JAR (local dev defaults)
2. classpath:/config/
3. file:./                        ← filesystem, same dir as java -jar ← THIS WINS
4. file:./config/
```

The container runs `java -jar /opt/kanbana/kanbana.jar` from `/opt/kanbana/`. Ansible writes `application.properties` to `/opt/kanbana/application.properties`. Spring Boot finds it at `file:./` and uses it instead of thd one. The JAR stays generic — no environment-specific config baked in.

**Two `application.properties` files — different purposes:**

```
src/main/resources/application.properties     ← bundled in JAR, local dev
                                                 uses ${VAR:default} fallbacks
                                                 committed to git

infra/ansible/roles/appserver/templates/      ← Ansible template, deployed to server
    application.properties                       uses {{ var substitution
                                                 real values written at deploy time
                                                 never committed with real secrets
```

**Development only: `application-test.properties`**

Only loaded when `@ActiveProfiles("test")` is active — during `mvn test` only. The deployed container runs `java -jar` with no profiles, so Spring ignores it entirely.

---

### Final structure

```
infra/ansible/
    inventories/
        hosts.ini
        group_vars/
          all/
                vars.yml        ← db_name, db_user, jdbc_url
                vault.yml       ← db_password (encrypted)
    roles/
        docker/                 ← unchanged
        db/
            tasks/main.yml      ← unchanged
        appserver/
            tasks/main.yml      ← unchanged, creates container skeleton only
            templates/
                application.properties   ← Ansible template
    playbooks/
        app-server.yml          ← unchanged
        db-server ← unchanged
        deploy.yml              ← new
```

---

### deploy.yml — explained

File: `infra/ansible/playbooks/deploy.yml`

Tasks:
1. Ensure `/opt/kanbana` exists
2. Copy JAR from `target/kanbana.jar` to `/opt/kanbana/kanbana.jar`
3. Write `application.properties` from template with `mode: 0600`
4. Restart container to pick up new JAR and config
5. Health check — poll `GET /api/v1/boards` until 200, retry 10x with 5s delay

---

### Step 2 — Build and deploy

```bash
# Build JAR
cd ~/kanbean package -DskipTests

# Deploy
cd infra
ansible-playbook --ask-vault-pass ansible/playbooks/deploy.yml
```

---

### Step 3 — Smoke test

```bash
# POST a board
curl -s -X POST http://172.27.155.44:8080/api/v1/boards \
  -H "Content-Type: application/json" \
  -d '{"title":"Deploy Test"}' | jq

# Verify row landed in Postgres on db-server
multipass shell db-server
docker exec -it postgres psql -U kanbana -d kanbana \
  -c "SELECT * FROM kanbana.boards;"
```

Expected: POST returns 201 with JSON body coaining `id` and `title`. SELECT returns one row.

---

## 9.11 — Rebuild from Scratch

> VMs deleted or IPs changed. Full sequence to get everything back up and deployed in one go.

---

### Prerequisites

- WSL2 running, Ansible installed
- Multipass installed on Windows
- Repo cloned at `~/kanbana`
- Vault password known

---

### Step 1 — Launch VMs

Run from PowerShell or WSL:

```bash
multipass launch -n app-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-initud-init.yaml \
  --mount C:/Users/alyso/multipass_mount:/mnt/multipass_mount

multipass launch -n db-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/alyso/multipass_mount:/mnt/multipass_mount
```

---

### Step 2 — Update inventory with new IPs

```bash
multipass list   # get current IPs
vim infra/ansible/inventories/hosts.ini
```

Update both `ansible_host=` values.

---

### Step 3 — Provision infrastructure

```bash
cd ~bana/infra

# Install Docker + create container skeleton on app-server
ansible-playbook ansible/playbooks/app-server.yml

# Install Docker + run Postgres + PgBouncer on db-server
ansible-playbook ansible/playbooks/db-server.yml --ask-vault-pass
```

---

### Step 4 — Build the JAR

```bash
cd ~/kanbana
mvn clean package -DskipTests
```

---

### Step 5 — Deploy

```bash
cd infra
ansible-playbook --ask-vault-pass ansible/playbooks/deploy.yml
```

This copies the JAR, writes `application.properties` with  DB credentials, restarts the container, and waits for the health check to pass.

---

### Step 6 — Verify

```bash
# POST a board
curl -s -X POST http://<app-server-ip>:8080/api/v1/boards \
  -H "Content-Type: application/json" \
  -d '{"title":"Rebuild Test"}' | jq

# Confirm row in Postgres
multipass shell db-server
docker exec -it postgres psql -U kanbana -d kanbana \
  -c "SELECT * FROM kanbana.boards;"
```

---

### Notes

- Flyway runs automatically on startup — schema is recreated from migration a fresh DB
- Data does not survive VM deletion — the Docker volume is gone with the VM
- IPs are DHCP — always run `multipass list` and update `hosts.ini` before running any playbook
- Vault password is required for `db-server.yml` and `deploy.yml`
