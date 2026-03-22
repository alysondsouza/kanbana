# PHASE 4 — Frontend (React + Vite)

---

## Prerequisites

Before starting Phase 4, verify these are installed in WSL:

```bash
node --version   # must be v18+  (we use v22)
npm --version    # must be v8+   (we use v11)
```

If missing or outdated:

```bash
# Remove old versions
sudo apt remove nodejs npm -y

# Install Node.js 22 LTS via NodeSource (official Ubuntu distribution)
sudo apt install -y ca-certificates curl gnupg
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs

# Verify
node --version   # v22.x.x
npm --version    # 11.x.x
```

> Node and npm stay on WSL only — not on the VMs.
> The frontend builds to static files (`dist/`) and is served by Cloudflare Pages.

---

## Architecture

```
Windows 11
    └── WSL2 (Ubuntu 24.04)
            ├── Java 21 + Maven    → builds kanbana.jar → deployed to app-server VM
            └── Node.js 22 + npm   → builds frontend/dist/ → deployed to Cloudflare Pages

Multipass (Hyper-V)
    ├── vm: app-server             → Docker → Spring Boot REST API (port 8080)
    └── vm: db-server              → Docker → PostgreSQL + PgBouncer

Cloudflare Pages (free CDN)
    └── serves frontend/dist/      → built from GitHub on every push to main
```

**Traffic flow (production):**
```
Browser → Cloudflare Pages → React app (static files)
        → app-server VM:8080 → Spring Boot API → PgBouncer → PostgreSQL
```

**During development — two processes must run simultaneously:**
```
Terminal 1: Spring Boot (port 8080)     ← API
Terminal 2: Vite dev server (port 5173) ← Frontend

Browser → Vite (172.25.x.x:5173)
        → Vite proxy → Spring Boot (localhost:8080)
```

> Both must be running at the same time. Killing one makes the other half-functional.

---

## Tech Stack

| Tool | Purpose |
|---|---|
| React 19 + Vite | UI framework + build tool |
| Tailwind CSS v4 | Utility-first CSS |
| react-router-dom | Client-side routing between pages |
| axios | HTTP client for API calls |
| @dnd-kit | Drag and drop (cards between columns) |
| Cloudflare Pages | Free static hosting, deploys from GitHub |

---

## Repository Structure

```
kanbana/
├── frontend/                      ← React + Vite app (this phase)
│   ├── public/
│   ├── src/
│   │   ├── api/
│   │   │   └── client.js          ← axios instance + JWT header injection
│   │   ├── context/
│   │   │   └── AuthContext.jsx    ← JWT stored in memory, shared via context
│   │   ├── pages/
│   │   │   ├── LoginPage.jsx
│   │   │   ├── RegisterPage.jsx
│   │   │   ├── BoardListPage.jsx
│   │   │   └── BoardViewPage.jsx
│   │   ├── components/
│   │   │   ├── Column.jsx
│   │   │   └── CardItem.jsx
│   │   ├── App.jsx                ← routing
│   │   ├── main.jsx               ← entry point
│   │   └── index.css              ← Tailwind import
│   ├── index.html
│   ├── vite.config.js
│   └── package.json
├── src/                           ← Spring Boot backend
├── infra/                         ← Ansible
└── NOTE_4_FRONTEND.md             ← this file
```

---

## Phase 4 Checklist

- [x] 11.1 — Branch + CORS
- [x] 11.2 — Vite scaffold
- [x] 11.3 — Auth pages (Login + Register)
- [x] 11.4 — Board list page
- [x] 11.5 — Board view (columns + cards)
- [x] 11.6 — Drag and drop
- [ ] 11.7 — Deploy to Cloudflare Pages

---

## Chapter 11 — Frontend

### 11.1 — Branch + CORS

#### Branch

```bash
cd ~/kanbana
git checkout -b feat/phase4-frontend
```

#### What is CORS?

CORS (Cross-Origin Resource Sharing) is the mechanism that lets a server explicitly
allow requests from a different origin (protocol + host + port combination).

**The problem:** the browser's Same-Origin Policy blocks requests between different
origins by default — a security rule to prevent malicious sites from silently
calling APIs using the user's identity.

**Why Postman doesn't care:** Postman is not a browser. It has no Same-Origin Policy.
It sends HTTP requests directly, with no preflight and no origin checks. CORS only
applies when a browser makes a cross-origin request.

**How it works:**
```
Browser (172.25.x.x:5173) wants to call API (localhost:8080)
    ↓
Sends preflight:   OPTIONS /api/v1/boards
                   Origin: http://172.25.x.x:5173
                   Access-Control-Request-Method: GET
    ↓
Server responds:   Access-Control-Allow-Origin: http://172.25.x.x:5173
                   Access-Control-Allow-Methods: GET,POST,PATCH,DELETE,OPTIONS
    ↓
Browser: "server said ok" → sends real request
```

If the server does not respond with those headers → browser blocks the request.
The Java code never runs. curl and Postman are unaffected.

**Key rules:**
- Wildcard `*` is rejected by browsers when `allowCredentials = true` — must name exact origins
- `OPTIONS` must be in allowed methods — the preflight uses it
- CORS must be configured in Spring Security, not just Spring MVC — Security runs first

#### Why localhost didn't work

When Vite runs in WSL and the browser is on Windows, requests arrive at Spring Boot
with the WSL network IP as origin (`http://172.25.x.x:5173`), not `http://localhost:5173`.
Windows resolves `localhost` to its own `127.0.0.1`, not WSL's.

#### The fix — ALLOWED_ORIGINS env var (12-Factor App)

File: `src/main/java/com/kanbana/api/SecurityConfig.java`

```java
String allowedOriginsEnv = System.getenv()
        .getOrDefault("ALLOWED_ORIGINS", "http://localhost:5173");
List<String> allowedOrigins = Arrays.asList(allowedOriginsEnv.split(","));
config.setAllowedOrigins(allowedOrigins);
```

| Environment | Command |
|---|---|
| Dev (WSL) | `ALLOWED_ORIGINS="http://172.25.36.218:5173" mvn spring-boot:run` |
| Prod | `ALLOWED_ORIGINS="https://kanbana.pages.dev"` (Ansible — 11.7) |

#### Known gap — deploy.yml

`deploy.yml` does not yet inject `ALLOWED_ORIGINS` into the Docker container.
Wired up in 11.7 when the Cloudflare Pages URL is known.

#### Verify

```bash
curl -v -X OPTIONS http://localhost:8080/api/v1/boards \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET"
```

Expected:
```
Access-Control-Allow-Origin: http://localhost:5173   ✅
Access-Control-Allow-Methods: GET,POST,PATCH,DELETE,OPTIONS   ✅
Access-Control-Allow-Credentials: true   ✅
```

---

### 11.2 — Vite Scaffold

#### What is Vite?

Vite is a build tool and dev server for frontend projects. It serves your React
files during development with instant hot reload (HMR — Hot Module Replacement),
and bundles everything into optimised static files (`dist/`) for production.

```
Development:   src/ → Vite dev server → browser (live reload on save)
Production:    src/ → npm run build  → dist/ → Cloudflare Pages
```

#### Packages installed

| Package | Purpose |
|---|---|
| `vite` + `react` | Build tool + UI framework (scaffolded by create-vite) |
| `react-router-dom` | Client-side routing between pages |
| `axios` | HTTP client — makes API calls to Spring Boot |
| `@dnd-kit/core` | Drag and drop core engine |
| `@dnd-kit/sortable` | Sortable preset for dnd-kit (cards in columns) |
| `@dnd-kit/utilities` | Helper utilities for dnd-kit |
| `tailwindcss` | Utility-first CSS framework |
| `@tailwindcss/vite` | Tailwind v4 Vite plugin (replaces postcss config) |

#### Steps

```bash
cd ~/kanbana
npx create-vite@latest frontend --template react
# Answer "No" to "Install with npm and start now?"

cd frontend
npm install
npm install axios
npm install react-router-dom
npm install @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities
npm install -D tailwindcss @tailwindcss/vite
```

#### Config files

`frontend/vite.config.js`:
```js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    proxy: { '/api': 'http://localhost:8080' }
  }
})
```

`frontend/src/index.css`:
```css
@import "tailwindcss";
```

#### Running the dev server

```bash
ip addr show eth0 | grep "inet "   # find WSL IP
cd ~/kanbana/frontend
npm run dev                         # or: npm run dev -- --host
# open http://<WSL-IP>:5173 in Windows browser
```

#### Issues encountered

| Problem | Cause | Fix |
|---|---|---|
| `EPERM mkdir C:\Windows\frontend` | Windows npm taking priority | Use `npx create-vite` instead |
| `Cannot find native binding` (rolldown) | npm optional dependency bug | `rm -rf node_modules package-lock.json && npm install` |
| `Permission denied` on vite binary | File permissions lost | `chmod +x node_modules/.bin/vite` |
| `localhost:5173` not reachable from Windows | WSL bridged mode | Use WSL IP instead |
| Vite returns empty response | Not bound to network interface | Add `host: '0.0.0.0'` to vite.config.js |

---

### 11.3 — Auth Pages

#### What we built

Login and Register pages connected to Spring Boot auth endpoints.
JWT stored in React context (in memory — never localStorage).

```
POST /api/v1/auth/register  →  RegisterPage  → navigate('/boards')
POST /api/v1/auth/login     →  LoginPage     → navigate('/boards')
```

#### Files

```
src/context/AuthContext.jsx   ← { token, login, logout } shared via context
src/api/client.js             ← axios instance + setAuthToken()
src/pages/LoginPage.jsx
src/pages/RegisterPage.jsx
src/App.jsx                   ← routing + ProtectedRoute
```

#### ProtectedRoute

```jsx
function ProtectedRoute({ children }) {
  const { token } = useAuth()
  return token ? children : <Navigate to="/login" replace />
}
```

Token present → render page. No token → redirect to `/login`.
Page refresh = token gone = back to login (memory only).

#### Running locally

```bash
# Terminal 1
cd ~/kanbana
DB_HOST=172.25.42.203 DB_PASSWORD=123 \
ALLOWED_ORIGINS="http://172.25.36.218:5173" \
mvn spring-boot:run

# Terminal 2
cd ~/kanbana/frontend
npm run dev
```

#### Issues encountered

| Problem | Cause | Fix |
|---|---|---|
| `Registration failed` | Spring Boot not running | Start both servers |
| `origin is not allowed` | WSL IP missing from ALLOWED_ORIGINS | Pass correct IP as env var |

---

### 11.4 — Board List Page

#### What we built

Page at `/boards` — fetches all boards, renders as clickable grid,
create new board, logout button.

```
GET  /api/v1/boards   → load on mount
POST /api/v1/boards   → create new board (optimistic update)
```

#### Key concepts

**useEffect with `[]`** — runs once on mount, fetches boards.

**Optimistic UI** — append new board to local state immediately after POST,
without re-fetching the full list.

---

### 11.5 — Board View

#### What we built

Page at `/boards/:id` — fetches columns and cards, renders Kanban layout,
create columns and cards.

```
GET  /api/v1/boards/:id/columns     → load columns
GET  /api/v1/columns/:id/cards      → load cards per column (parallel)
POST /api/v1/boards/:id/columns     → create column
POST /api/v1/columns/:id/cards      → create card
```

#### Files

```
src/pages/BoardViewPage.jsx
src/components/Column.jsx
src/components/CardItem.jsx
```

#### Promise.all — parallel card fetching

```jsx
const cardEntries = await Promise.all(
  cols.map(async col => {
    const res = await client.get(`/api/v1/columns/${col.id}/cards`)
    return [col.id, res.data]
  })
)
setCards(Object.fromEntries(cardEntries))
```

All column card requests fire simultaneously — faster than sequential fetching.

#### Backend bugs found and fixed

`ObjectOptimisticLockingFailureException` on create column and create card.
Cause: `toEntity()` was passing the real UUID — Spring Data ran UPDATE instead of INSERT.

| File | Fix |
|---|---|
| `BoardColumnMapper.java` | `toEntity()` — `column.getId()` → `null` |
| `CardMapper.java` | `toEntity()` — `card.getId()` → `null` |

---

### 11.6 — Drag and Drop

#### What we built

Cards are draggable between columns using `@dnd-kit`.
On drop: optimistic UI update + `PATCH /api/v1/cards/:id/move`.
On failure: reverts to original state.

#### How dnd-kit works

```
<DndContext onDragStart onDragEnd>     ← detects drag events across all columns
  <Column>
    <SortableContext items={cardIds}>  ← defines sortable order within a column
      <CardItem>                       ← useSortable() makes each card draggable
    </SortableContext>
  </Column>
  <DragOverlay>                        ← floating copy follows the cursor
    <CardItem card={activeCard} />
  </DragOverlay>
</DndContext>
```

#### DragOverlay

Without it: card disappears from column while dragging.
With it: original card stays faded in column + floating copy follows the cursor.

```jsx
const handleDragStart = ({ active }) => {
  const card = Object.values(cards).flat().find(c => c.id === active.id)
  setActiveCard(card ?? null)
}

<DragOverlay>
  {activeCard ? <CardItem card={activeCard} /> : null}
</DragOverlay>
```

#### Resolving targetColumnId

`over.id` from dnd-kit can be either a column ID (dropped on column background)
or a card ID (dropped on top of another card). We resolve it:

```jsx
const targetColumnId =
  Object.keys(cards).find(colId =>
    cards[colId].some(c => c.id === over.id)
  ) ?? over.id
```

#### Issues encountered

| Problem | Cause | Fix |
|---|---|---|
| `Target column not found: uuid` | `over.id` was a card UUID, not column UUID | Resolve `targetColumnId` from card lookup with `?? over.id` fallback |
| Card disappears while dragging | No DragOverlay configured | Add `DragOverlay` + `onDragStart` to track `activeCard` |

---

### 11.7 — Deploy to Cloudflare Pages

#### Plan

**1. Build check (WSL)**
```bash
cd ~/kanbana/frontend
npm run build   # produces dist/ — verify no errors
```

**2. Push branch and open PR**
```bash
cd ~/kanbana
git push origin feat/phase4-frontend
# open PR on GitHub → merge to main
```

**3. Connect Cloudflare Pages to GitHub**
- Sign up at cloudflare.com (free)
- Pages → Create project → Connect GitHub → select `kanbana` repo
- Build settings:
  - Root directory: `frontend`
  - Build command: `npm run build`
  - Output directory: `dist`
- Deploy → Cloudflare gives you a URL: `https://kanbana.pages.dev`

**4. Update CORS in SecurityConfig**
Add the Cloudflare URL to `ALLOWED_ORIGINS` in `deploy.yml`:
```yaml
ALLOWED_ORIGINS: "https://kanbana.pages.dev"
```

**5. Update deploy.yml**
Inject `ALLOWED_ORIGINS` as a Docker env var in the container restart task.

**6. Redeploy backend**
```bash
cd ~/kanbana/infra
ansible-playbook --ask-vault-pass ansible/playbooks/deploy.yml
```

**7. Update vite.config.js for production**
The Vite proxy only works in dev. In production the frontend calls the API directly.
Add the API base URL as an env var:
```js
server: { proxy: { '/api': 'http://localhost:8080' } }  // dev only
```

**8. Tag and close**
```bash
git tag v4.0-frontend
git push origin v4.0-frontend
```

---
