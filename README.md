# Kanbana

A full-stack Kanban board application built with Java, Spring Boot, and PostgreSQL.
This project is also a learning platform for DevOps, CI/CD, and cloud-native practices.

---

## Project Architecture

```
Windows 11
    ├── WSL2 (Ubuntu 24.04)            → dev terminal, Ansible control node
    │
    └── Multipass (Hyper-V)
            ├── vm: app-server         → Docker → Tomcat 11 / Spring Boot
            └── vm: db-server          → Docker → PostgreSQL + PgBouncer
```

---

## Tech Stack

### Infrastructure
| Tool | Purpose |
|---|---|
| Multipass | Lightweight Ubuntu VMs on Windows via Hyper-V |
| Docker | Container runtime on each VM |
| Ansible | Infrastructure automation |
| PgBouncer | PostgreSQL connection pooler |

### Application
| Tool | Purpose |
|---|---|
| Java 21 LTS | Backend language |
| Spring Boot | Embedded Tomcat / application framework |
| Tomcat 11 | Standalone web server (WAR deployment) |
| PostgreSQL | Relational database |
| Hibernate / JPA | ORM layer |

### CI/CD
| Tool | Purpose |
|---|---|
| GitHub Actions | Automated build and test on push |
| Jenkins | Self-hosted CI/CD pipelines |
| JFrog Artifactory | Artifact repository (JARs, WARs, Docker images) |

### Observability
| Tool | Purpose |
|---|---|
| Prometheus | Metrics collection |
| Grafana | Metrics dashboards |
| Loki | Log aggregation |

---

## Repository Structure

```
kanbana/
├── infra/
│   ├── ansible.cfg
│   ├── ansible/
│   │   ├── inventories/
│   │   │   └── hosts.ini              → VM IPs and SSH users
│   │   ├── playbooks/
│   │   │   ├── app-server.yml
│   │   │   └── db-server.yml
│   │   └── roles/
│   │       ├── docker/                → installs Docker on any VM
│   │       ├── tomcat/                → runs Tomcat 11 container
│   │       └── db/                    → runs Postgres + PgBouncer
│   └── cloud-init/
│       └── cloud-init.yaml            → VM bootstrap config
├── src/
│   └── main/
│       ├── java/
│       │   └── com/kanbana/
│       │       ├── controller/        → REST API controllers
│       │       ├── service/           → business logic
│       │       ├── repository/        → database layer
│       │       └── model/             → domain entities
│       └── resources/
│           ├── application.yml        → app config
│           └── db/
│               └── migrations/        → Flyway SQL migrations
└── README.md
```

---

## Phases

### ✅ Phase 1 — Infrastructure
- [x] WSL2 + Hyper-V setup on Windows 11
- [x] SSH key generation and GitHub integration
- [x] Multipass VMs — `app-server` and `db-server`
- [x] WSL ↔ Multipass networking (bridged mode)
- [x] Ansible installed and configured
- [x] Ansible Docker role — Docker installed on both VMs
- [ ] Ansible Tomcat role — Tomcat 11 container on app-server
- [ ] Ansible DB role — PostgreSQL + PgBouncer on db-server

### 🔲 Phase 2 — Application (Hello World → Full App)
- [ ] Hello World WAR deployed to standalone Tomcat
- [ ] Hello World with embedded Tomcat (Spring Boot) — compare the two approaches
- [ ] Project structure — Onion Architecture
- [ ] Domain model — Board, Column, Card, User
- [ ] REST API — CRUD for boards, columns, cards
- [ ] Database schema and Flyway migrations
- [ ] Spring Security — JWT authentication
- [ ] API testing with Postman / REST Assured

### 🔲 Phase 3 — CI/CD
- [ ] GitHub Actions — build and test on every push
- [ ] Jenkins — self-hosted pipeline
- [ ] JFrog Artifactory — artifact storage
- [ ] Automated deployment to app-server via Ansible

### 🔲 Phase 4 — Observability
- [ ] Prometheus — metrics from Spring Boot (Micrometer)
- [ ] Grafana — dashboards for JVM, HTTP, DB metrics
- [ ] Loki — centralized log aggregation
- [ ] Alerting rules in Grafana

---

## Getting Started

### Prerequisites
- Windows 11 with Hyper-V enabled
- WSL2 (Ubuntu 24.04)
- Multipass
- Ansible 2.12+

### Setup

**1. Clone the repo**
```bash
git clone git@github.com:alysondsouza/kanbana.git
cd kanbana
```

**2. Launch VMs**
```bash
multipass launch -n app-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/<your-user>/multipass_mount:/mnt/multipass_mount

multipass launch -n db-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/<your-user>/multipass_mount:/mnt/multipass_mount
```

**3. Run Ansible**
```bash
cd infra
ansible-playbook ansible/playbooks/app-server.yml
ansible-playbook ansible/playbooks/db-server.yml
```

---

## Branching Strategy

This project follows **GitHub Flow**:
- `main` — always deployable
- `feat/<name>` — new features
- `fix/<name>` — bug fixes
- `infra/<name>` — infrastructure changes

Open a PR to merge into `main`. Delete branches after merging.

---

## Notes & Decisions

| Decision | Reason |
|---|---|
| Multipass on Windows, not WSL | Avoids nested virtualization |
| WSL bridged networking | Puts WSL on same Hyper-V switch as Multipass VMs |
| PgBouncer on db-server | Connection pooler belongs next to the database |
| Onion Architecture | Clean separation of domain, application, and infrastructure layers |
| Flyway for migrations | Version-controlled, idempotent database schema changes |
