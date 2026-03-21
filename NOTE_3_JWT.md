# PHASE 3 — Authentication (Spring Security + JWT)

---

## Chapter 10 — Spring Security + JWT ✅

Users can register and log in. Every protected endpoint requires a valid JWT. Boards belong to their owner — users only see their own data.

**Public:** `POST /api/v1/auth/register` → 201, `POST /api/v1/auth/login` → 200
**Protected:** everything else → 401 without token

**New files:**
```
application/dto/         RegisterRequestDTO, LoginRequestDTO, AuthResponseDTO
application/service/     JwtService, AuthService, ConflictException
domain/repository/       UserRepository
infrastructure/          UserJpaRepository, UserRepositoryAdapter, UserMapper
api/                     AuthController, JwtAuthFilter, SecurityConfig
db/migration/            V5__add_owner_fk.sql
```

---

### How JWT works

A JWT has three parts: `header.payload.signature`. The server signs it with a secret key. On every request, the server re-computes the signature — if it matches, the token is genuine. No database lookup needed. That is what stateless means.

```
Login:
  POST /auth/login → AuthService verifies BCrypt hash
                   → JwtService.generateToken(userId, username)
                   → { "token": "eyJ..." }

Every protected request:
  Authorization: Bearer eyJ...
    → JwtAuthFilter — validates signature + expiry
    → SecurityContextHolder ← userId set as principal
    → BoardService.getCurrentUserId() reads it → owner_id
```

The payload contains the user's UUID (`sub`) and username. Expiry is 24 hours.

---

### How the pieces connect

| Class | Layer | Role |
|---|---|---|
| `JwtService` | Application | Generates and validates tokens |
| `AuthService` | Application | Register (BCrypt + save), Login (verify + token) |
| `JwtAuthFilter` | API | Runs on every request, sets SecurityContext |
| `SecurityConfig` | API | Declares public vs protected endpoints, BCrypt bean |
| `AuthController` | API | Two endpoints — delegates to AuthService |
| `UserRepository` | Domain | Port — findByUsername, existsByUsername, existsByEmail |
| `UserRepositoryAdapter` | Infrastructure | Implements domain port via JPA |

---

### Security decisions

**BCrypt cost 12** — ~250ms per hash. Slow enough to resist brute force.

**No username enumeration (OWASP)** — wrong password and unknown user return the same message: `"Invalid credentials"`. Different messages let attackers discover valid usernames.

**Stateless** — no sessions, no JSESSIONID. Any app server validates any token with the shared secret. Load balancer + 2 app servers (Phase 5) works with no changes.

**JWT_SECRET** — the secret key is what makes JWT signatures trustworthy. If someone knows it, they can forge any token and your server accepts it as a valid login. Never hardcode it — if the repo ever leaks, every token can be forged. Dev falls back to a safe default. Production value lives in Ansible vault, injected into the Docker container as an env var at deploy time. Config that varies between environments lives in the environment, not in the code (12-Factor App).

---

### Commands

```bash
#DEV
DB_HOST=172.25.42.203 DB_PASSWORD=123 mvn clean spring-boot:run

curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"password123"}' | jq

curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' | jq

curl -s http://localhost:8080/api/v1/boards \
  -H "Authorization: Bearer <token>" | jq

#PROD
mvn clean test
mvn clean package -DskipTests
cd infra
ansible-playbook --ask-vault-pass ansible/playbooks/deploy.yml
```

---

### Phase 5 — OpenLDAP (planned)

LDAP handles credential verification — if LDAP says the password is correct, `AuthService` still issues a JWT. Spring Security supports multiple authentication providers simultaneously, so the existing username/password flow stays intact alongside LDAP. Mobile clients and the load-balanced setup are unaffected — they only ever see JWT.
