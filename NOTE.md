# PHASE 1 — Infrastructure

## Architecture

```
Windows 11
    ├── WSL2 (Ubuntu 24.04)          → dev terminal, Ansible
    │
    └── Multipass (Hyper-V)
            ├── vm: app-server       → Docker → Tomcat 11
            └── vm: db-server        → Docker → Postgres
                                     → Docker → PgBouncer
```

---

## Summary

- [x] Install WSL & enable Hyper-V
- [x] SSH Key setup
- [x] Install Multipass
- [x] Launch Multipass VMs (app-server, db-server)
- [x] WSL ↔ Multipass networking (bridged mode)
- [x] Configure Ansible
- [x] Docker installed and verified on app-server
- [ ] Run Ansible on db-server
- [x] Postgres + PgBouncer running on db-server
- [ ] Tomcat 11 running on app-server

---

# Chapter 1 — WSL & Hyper-V

## Step 1 — Install WSL from Microsoft Store

Search for **Ubuntu 24.04.6 LTS** in the Microsoft Store and install it.

---

## Step 2 — Enable Required Windows Features

Open **PowerShell as Administrator** and run:

```powershell
# Enable WSL
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart

# Enable Virtual Machine Platform (required for WSL2)
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart

# Enable Hypervisor Platform (required for Docker & Multipass)
dism.exe /online /enable-feature /featurename:HypervisorPlatform /all /norestart

# Enable full Hyper-V stack — then reboot
dism /online /enable-feature /featurename:Microsoft-Hyper-V-All /all
```

> **Why?** WSL2 runs a real Linux kernel inside a lightweight VM — it needs Virtual Machine Platform, Hypervisor, and Hyper-V enabled. Reboot after this step.

---

## Step 3 — Configure & Verify Hypervisor

```powershell
# Check current hypervisor launch type
bcdedit /enum | findstr hypervisorlaunchtype

# Set to auto (must be 'auto' for Hyper-V and WSL2 to work)
bcdedit /set hypervisorlaunchtype auto

# Check each feature state — should return "Enabled"
Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux
Get-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform
Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All

# Verify Hyper-V compute service is running
Get-Service vmcompute

# Confirm hypervisor is detected by the OS
systeminfo | findstr /i hypervisor
```

> **Common mistake:** If `hypervisorlaunchtype` is set to `off`, Docker Desktop and Multipass will fail silently.

---

## Step 4 — Update Ubuntu (inside WSL)

```bash
sudo apt update && sudo apt upgrade
```

---

# Chapter 2 — SSH

## Step 1 — SSH Key Setup (inside WSL)

```bash
# Generate SSH key (ed25519 is modern and secure)
ssh-keygen -t ed25519 -C "your_github_email@example.com"

# Start SSH agent and add key
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# Print public key — copy this to GitHub
cat ~/.ssh/id_ed25519.pub

# Test GitHub connection
ssh -T git@github.com

# Clone your repo via SSH
git clone git@github.com:alysondsouza/kanbana.git
```

> **GitHub:** Settings → SSH and GPG keys → New SSH key → paste public key.

---

# Chapter 3 — Multipass

## Step 1 — Set Network to Private

> Windows blocks Multipass on public networks.

1. Click the **WiFi/Ethernet icon** in the taskbar (bottom right)
2. Click **Properties** on your current network
3. Under **Network profile type** → select **Private network**

---

## Step 2 — Install Multipass (Windows)

> **Why on Windows and not WSL?** Multipass manages VMs directly through Hyper-V. WSL2 is itself a VM — installing Multipass inside it would cause nested virtualization (a VM inside a VM), which is unsupported and causes networking issues.

Install via winget (PowerShell as Administrator):
```powershell
winget install Canonical.Multipass
```

Verify:
```powershell
multipass version
```

---

## Step 3 — Check Available RAM

Before allocating VM resources, check how much RAM and disk are available:

```powershell
# RAM
systeminfo | findstr /i "Total Physical Memory"

# Disk
Get-PSDrive C | Select-Object Used, Free
Get-PSDrive D | Select-Object Used, Free
```

---

## Step 4 — Move VM Storage to D:\ (if C:\ is low on space)

> Each VM disk image is 4GB+. Move storage to a drive with more space.

```powershell
# Set system environment variable (PowerShell as Administrator)
[System.Environment]::SetEnvironmentVariable("MULTIPASS_STORAGE", "D:\multipass", "Machine")

# Verify
[System.Environment]::GetEnvironmentVariable("MULTIPASS_STORAGE", "Machine")

# Restart Multipass service
Restart-Service Multipass
```

> **Note:** Close and reopen PowerShell after setting the variable for it to take effect.

---

## Step 5 — Enable WSL Interop

Required so WSL can call Windows `.exe` binaries like `multipass.exe`.

```bash
# Add interop config to wsl.conf
sudo vim /etc/wsl.conf
```

Ensure the file contains:
```ini
[boot]
systemd=true

[interop]
enabled=true
appendWindowsPath=true
```

Restart WSL from PowerShell:
```powershell
wsl --shutdown
```

---

## Step 6 — Link Multipass to WSL

```bash
# Create symlink so WSL can call the Windows binary
sudo ln -s '/mnt/c/Program Files/Multipass/bin/multipass.exe' /usr/bin/multipass

# Verify
multipass version
```

---

## Step 7 — Enable Privileged Mounts

Required to mount Windows folders into Multipass VMs.

```powershell
# PowerShell as Administrator
multipass set local.privileged-mounts=true
```

---

## Step 8 — Create Mount Folder & cloud-init

Create the shared mount folder on Windows:
```
C:\Users\alyso\multipass_mount\
```

Create `cloud-init.yaml` in your repo at `infra/cloud-init/cloud-init.yaml`:
```yaml
#cloud-config
users:
  - default
  - name: ubuntu
    sudo: "ALL=(ALL) NOPASSWD:ALL"
    shell: /bin/bash
    ssh_authorized_keys:
      - <paste your id_ed25519.pub here>

package_update: true
package_upgrade: true

packages:
  - curl
  - git
  - vim
```

---

## Step 9 — Launch VMs

```bash
# Check available networks
multipass networks

# Launch app-server
multipass launch -n app-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/alyso/multipass_mount:/mnt/multipass_mount

# Launch db-server
multipass launch -n db-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/alyso/multipass_mount:/mnt/multipass_mount

# Verify both are running
multipass list
```

Expected output:
```
Name                    State             IPv4             Image
app-server              Running           172.23.223.89    Ubuntu 24.04 LTS
db-server               Running           172.23.220.152   Ubuntu 24.04 LTS
```

---

## Step 10 — Verify VMs

```bash
# Shell into a VM
multipass shell app-server

# Verify inside the VM
whoami           # ubuntu
sudo whoami      # root
git --version
curl --version
vim --version
```

---

# Chapter 4 — Networking (WSL ↔ Multipass)

## Step 1 — Diagnose the Network Isolation

Before fixing, confirm which networks WSL and Multipass are on:

```bash
# WSL — check its IP and gateway
ip route
```

Expected output:
```
default via 172.27.192.1 dev eth0
172.27.192.0/20 dev eth0 src 172.27.196.159
```

```powershell
# Windows — check the WSL and Default Switch adapter IPs
Get-NetIPAddress | Where-Object { $_.InterfaceAlias -like "*WSL*" }
Get-NetIPAddress | Where-Object { $_.InterfaceAlias -like "*Default Switch*" }
```

This reveals the problem:
```
WSL adapter (vEthernet WSL):            172.27.192.1
Default Switch (vEthernet Default):     172.23.208.1
```

WSL and Multipass are on **two different Hyper-V switches** — isolated from each other. A simple ping confirms it:

```bash
ping 172.23.220.152  # 100% packet loss from WSL
```

```powershell
ping 172.23.220.152  # works fine from Windows
```

---

## Step 2 — Bridge WSL to the Multipass Network

By default WSL2 runs on its own private NAT network (`172.27.x.x`), isolated from the Multipass network (`172.23.x.x`). Bridged mode moves WSL onto the same Hyper-V switch as Multipass so they can communicate directly.

```
BEFORE:
    WSL       → 172.27.x.x  (private WSL switch)
    Multipass → 172.23.x.x  (Default Switch)

AFTER:
    WSL       → 172.23.x.x  (same Default Switch)
    Multipass → 172.23.x.x  ✅ same network
```

Create or edit `.wslconfig` in your Windows user folder:

```powershell
notepad C:\Users\alyso\.wslconfig
```

Add:
```ini
[wsl2]
networkingMode=bridged
vmSwitch=Default Switch
```

**What this means:**

- `.wslconfig` is the Windows-side config file for the WSL2 VM — controls memory, CPU, and networking
- `networkingMode=bridged` — switches WSL2 from NAT mode (own private network) to bridged mode (joins an existing switch directly)
- `vmSwitch=Default Switch` — tells WSL2 which Hyper-V switch to join — the same one Multipass uses



Restart WSL:
```powershell
wsl --shutdown
```

---

## Step 3 — Verify Connectivity

```bash
# Ping VMs from WSL
ping 172.23.223.89    # app-server
ping 172.23.220.152   # db-server

# SSH into VMs from WSL
ssh ubuntu@172.23.223.89
ssh ubuntu@172.23.220.152
```

---

# Chapter 5 — Ansible

## Architecture

```
infra/
└── ansible/
    ├── inventories/
    │   └── hosts.ini        → VM IPs + SSH user
    ├── playbooks/
    │   ├── app-server.yml   → roles for app-server
    │   └── db-server.yml    → roles for db-server
    └── roles/
        ├── docker/          → installs Docker (runs on both VMs)
        ├── tomcat/          → runs Tomcat 11 container
        └── db/              → runs Postgres + PgBouncer containers
```

---

## Step 1 — Install Ansible (WSL)

```bash
# Add official Ansible PPA for latest version
sudo apt-add-repository --yes --update ppa:ansible/ansible
sudo apt install ansible -y

# Verify
ansible --version  # should be 2.12+
```

---

## Step 2 — Create Inventory

```bash
vim infra/ansible/inventories/hosts.ini
```

```ini
[app_server]
app-server ansible_host=<app-server-ip> ansible_user=ubuntu

[db_server]
db-server ansible_host=<db-server-ip> ansible_user=ubuntu
```

> ⚠️ IPs are assigned by DHCP and change on restart. Always verify with `multipass list` and update `hosts.ini` accordingly. Static IPs will be configured later.



---

## Step 3 — Create ansible.cfg

Place `ansible.cfg` in `infra/` so all paths resolve relative to the ansible folder:

```bash
vim ~/kanbana/infra/ansible.cfg
```

```ini
[defaults]
inventory = ansible/inventories/hosts.ini
roles_path = ansible/roles
```

Run playbooks from `infra/`:
```bash
cd ~/kanbana/infra
ansible-playbook ansible/playbooks/app-server.yml
```

---

## Step 4 — Role: Docker

File: `infra/ansible/roles/docker/tasks/main.yml`

This role installs Docker Engine on any VM it runs on. Used by both `app-server` and `db-server`.

Tasks:
1. Install dependencies (`apt-transport-https`, `ca-certificates`, `curl`)
2. Create Docker keyring directory
3. Download and install Docker GPG key
4. Add Docker apt repository
5. Install Docker Engine (`docker-ce`, `docker-ce-cli`, `containerd.io`)
6. Start and enable Docker service
7. Add `ubuntu` user to `docker` group

Run and verify:
```bash
cd ~/kanbana/infra

# Run on app-server
ansible-playbook ansible/playbooks/app-server.yml

# Run on db-server
ansible-playbook ansible/playbooks/db-server.yml
```

Verify Docker inside each VM:
```bash
multipass shell app-server
docker --version          # Docker version 29.3.0
docker run hello-world    # confirms Docker can pull and run containers
exit

multipass shell db-server
docker --version
docker run hello-world
```

---

## Step 5 — Role: Tomcat

> **Note on networking:** Docker networks are VM-local. `kanbana-net` exists only on `db-server` — `app-server` cannot join it. Tomcat reaches Postgres via the VM IP (`db-server` IP) on port `6432` (PgBouncer). No shared Docker network needed between VMs.

Files:
- `infra/ansible/roles/tomcat/tasks/main.yml` — tasks

**What it does:**
1. Creates a Docker volume for WAR deployments (`tomcat-webapps`)
2. Runs Tomcat 11 container on port `8080`

**Verify:**
```bash
multipass shell app-server
docker ps
curl http://localhost:8080
```

## Step 6 — Role: DB — Postgres + PgBouncer

Files:
- `infra/ansible/roles/db/tasks/main.yml` — tasks
- `infra/ansible/roles/db/vars/main.yml` — vars + inline encrypted secret (Ansible Vault)

**What it does:**
1. Creates a Docker network (`kanbana-net`) — containers find each other by name, not IP
2. Creates a Docker volume for persistent Postgres data
3. Runs Postgres 17 container on port `5432` (internal only)
4. Runs PgBouncer 1.23.1 container on port `6432` (apps connect here)

**Port convention:**
- Apps always connect to **PgBouncer on 6432** — never directly to Postgres
- PgBouncer proxies connections to **Postgres on 5432** internally

**Persistent volume:**
```
db-server VM
└── /var/lib/docker/volumes/postgres-data/_data/  ← real data lives here
        ↑
        │ Docker mounts this as
        ↓
    Container
    └── /var/lib/postgresql/data  ← Postgres thinks data lives here
```
Data survives container restarts and recreation. Lost only if the VM is deleted.

**Ansible Vault:**

Use `encrypt_string` to encrypt the password inline directly in `vars/main.yml`:
```bash
ansible-vault encrypt_string 'your_password' --name db_password
```

Paste the output into `vars/main.yml`:
```yaml
db_name: kanbana
db_user: kanbana
db_password: !vault |
  $ANSIBLE_VAULT;1.1;AES256
  ...
```

Run playbook with vault:
```bash
ansible-playbook --ask-vault-pass ansible/playbooks/db-server.yml
```

**Verify:**
```bash
multipass shell db-server

# Both containers running
docker ps

# Postgres logs — confirm DB initialized
docker logs postgres

# Confirm kanbana database exists
docker exec -it postgres psql -U kanbana -d kanbana -c "\l"
```

Expected containers:
```
IMAGE                          PORTS                              NAMES
edoburu/pgbouncer:v1.25.1-p0   5432/tcp, 0.0.0.0:6432->6432/tcp   pgbouncer
postgres:17                    0.0.0.0:5432->5432/tcp             postgres
```

Traffic flow:
```
App → VM:6432 → PgBouncer:6432 → Postgres:5432
```

**Verify:**
```bash
multipass shell db-server
docker ps                          # postgres and pgbouncer containers running
docker logs postgres               # check for errors
docker exec -it postgres psql -U kanbana -d kanbana -c "\l"  # list databases
```

---

# Chapter 6 — Java & Maven (WSL)

## Step 1 — Install Java 21 LTS + Maven

```bash
sudo apt update
sudo apt install openjdk-21-jdk -y
sudo apt install maven -y
```

Verify:
```bash
java -version   # openjdk 21.x.x
mvn -version    # Apache Maven 3.x.x
```

## Step 2 — Verify JAVA_HOME

```bash
echo $JAVA_HOME
```

If empty, add to `~/.bashrc`:
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

Reload:
```bash
source ~/.bashrc
```

## Versions installed

| Tool  | Version  |
|-------|----------|
| Java  | 21.0.7   |
| Maven | 3.6.3    |


---


# Chapter 7 — Hello World WAR (Standalone Tomcat)

## Result
- GET /hello → "Hello World from Tomcat!"
- GET /bye   → "Ciao!"
- Accessible from Windows browser at http://<app-server-ip>:8080/hello
- Tagged in Git: v1.0-tomcat

---

## Project structure
```
kanbana/
├── pom.xml
└── src/main/
    ├── java/com/kanbana/
    │   ├── HelloServlet.java       ← handles GET /hello
    │   └── ByeServlet.java         ← handles GET /bye
    └── webapp/
        └── WEB-INF/
            └── web.xml             ← maps URLs to servlet classes
```

---

## How it works

```
Browser: GET /hello
              ↓
         Tomcat reads web.xml
              ↓
         /hello → helloServlet → com.kanbana.HelloServlet
              ↓
         Tomcat calls doGet()
              ↓
         "Hello World from Tomcat!"
```

- web.xml is the glue — maps URLs to classes
- Servlet classes have no idea what URL they are mapped to (Single Responsibility)
- doGet() is named after the HTTP verb, not the URL

---

## pom.xml — key decisions

| Setting | Value | Why |
|---|---|---|
| packaging | war | produces a .war file for Tomcat |
| finalName | ROOT | deploys to root context / instead of /kanbana |
| servlet-api scope | provided | Tomcat already has it at runtime |
| plugins | explicitly versioned | always pin plugin versions — Maven defaults are outdated |

---

## Build and deploy

```bash
# Build
mvn clean package                           # produces target/ROOT.war

# Copy to app-server
scp target/ROOT.war ubuntu@<ip>:/tmp/

# Shell into app-server
ssh ubuntu@<ip>

# Copy into running Tomcat container
docker cp /tmp/ROOT.war tomcat:/usr/local/tomcat/webapps/ROOT.war

# Tomcat auto-deploys — no restart needed
curl http://localhost:8080/hello
curl http://localhost:8080/bye
```

> docker cp is required — containers have their own filesystem.
> Regular cp cannot cross the container boundary.

---

## Rebuild from scratch (VMs deleted or IPs changed)

> IPs are assigned by DHCP — they change on every VM restart.
> Always run multipass list to get current IPs before connecting.

```bash
# 0. Git
git checkout v1.0-tomcat

# 1. Launch VMs
multipass launch -n app-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/alyso/multipass_mount:/mnt/multipass_mount

multipass launch -n db-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/alyso/multipass_mount:/mnt/multipass_mount

# 2. Get new IPs and update inventory
multipass list
vim infra/ansible/inventories/hosts.ini

# 3. Run Ansible — rebuilds Docker, Tomcat, Postgres, PgBouncer
cd infra
ansible-playbook ansible/playbooks/app-server.yml
ansible-playbook --ask-vault-pass ansible/playbooks/db-server.yml
# vault password: 123

# 4. Rebuild and redeploy WAR
cd ~/kanbana
mvn clean package
scp target/ROOT.war ubuntu@<ip>:/tmp/
ssh ubuntu@<ip>
docker cp /tmp/ROOT.war tomcat:/usr/local/tomcat/webapps/ROOT.war
curl http://localhost:8080/hello
```

---

## Issues encountered

| Problem | Cause | Fix |
|---|---|---|
| BUILD FAILURE — war plugin | maven-war-plugin 2.2 incompatible with Java 21 | Pin maven-war-plugin to 3.4.0 in pom.xml |
| 404 after redeploy | WAR in /tmp was stale from previous session | Always scp fresh WAR before docker cp |
| No route to host | VM IP changed after restart (DHCP) | Run multipass list and update hosts.ini |

---

## Key takeaways
- One servlet class per endpoint — scales poorly (30 endpoints = 30 classes)
- URL mapping lives in web.xml — separate from the servlet code
- WAR named ROOT.war gets the root context /
- Tomcat auto-deploys WARs dropped into webapps/ — no restart needed
- This approach is legacy but still common in enterprises
- Spring Boot (Option B) solves the one-class-per-endpoint problem


___


# Chapter 8 — Hello World (Embedded Tomcat via Spring Boot)

## Result
- `GET /hello` → `"Hello World from embedded Tomcat!"`
- `GET /bye` → `"Ciao!"`
- Accessible at `http://<app-server-ip>:8080/hello`
- Tagged in Git: `v1.0-embedded`

---

## vs Option A (WAR)

| | Option A (WAR) | Option B (JAR) |
|---|---|---|
| Server | External Tomcat in Docker | Tomcat inside your JAR |
| Deploy | Copy file into volume | `java -jar` |
| Config | `web.xml` | `application.properties` |
| Startup | Tomcat was already running | App starts its own server |
| Endpoints | One class per endpoint | One method per endpoint |
| Modern? | Legacy | Current standard |

---

## Generate project with Spring Initializr

> ⚠️ https://start.spring.io defaults to `4.0.3` — not LTS — works fine with Java 21.

```bash
curl https://start.spring.io/starter.tgz \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=4.0.3 \
  -d groupId=com.kanbana \
  -d artifactId=kanbana \
  -d name=kanbana \
  -d description="kanbana - embedded tomcat" \
  -d packageName=com.kanbana \
  -d packaging=jar \
  -d javaVersion=21 \
  -d dependencies=web \
  | tar -xzvf -
```

---

## What gets generated

```
kanbana/
├── pom.xml                                 ← Spring Boot config, packaging=jar
├── mvnw / mvnw.cmd                         ← Maven wrapper — ignore for now
├── HELP.md                                 ← can delete
└── src/
    ├── main/
    │   ├── java/com/kanbana/
    │   │   └── KanbanaApplication.java     ← entry point, starts embedded Tomcat
    │   └── resources/
    │       ├── application.properties      ← app config (port, db, etc)
    │       ├── static/                     ← frontend assets
    │       └── templates/                  ← server-side HTML templates
    └── test/
        └── java/com/kanbana/
            └── KanbanaApplicationTests.java
```

---

## What we add manually

- `AppController.java` ← `GET /hello` and `GET /bye` in one class

> One class handles all endpoints for a feature — in Option A (WAR) you needed a separate servlet class per endpoint, each registered in web.xml. 
> Here, one @RestController handles both /hello and /bye, and Spring registers them automatically via annotation scanning.

---

## application.properties

Location: `src/main/resources/application.properties`

Maven copies everything in `src/main/resources/` to the JAR root on `mvn package`.
Spring Boot reads `application.properties` from the classpath root on startup — no extra config needed.

```properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/kanbana
```

---

## Infrastructure — app-server

The JAR contains embedded Tomcat — no Tomcat container needed on app-server.

```
app-server VM → Docker → eclipse-temurin:21 → java -jar kanbana.jar
```

> `openjdk:21` is deprecated on Docker Hub — use `eclipse-temurin:21`.

Ansible `java` role (`infra/ansible/roles/java/tasks/main.yml`):
1. Creates `/opt/kanbana/` on the VM
2. Creates the container (stopped)
3. Copies `target/kanbana.jar` from WSL to `/opt/kanbana/kanbana.jar`
4. Starts the container

---

## Build and deploy

```bash
# 1. Build JAR in WSL
mvn clean package                           # produces target/kanbana.jar

# 2. Deploy via Ansible
cd infra
ansible-playbook ansible/playbooks/app-server.yml

# 3. Test
curl http://<ip>:8080/hello
curl http://<ip>:8080/bye
```

---

## Rebuild from scratch (VMs deleted or IPs changed)

```bash
# 1. Launch VMs
multipass launch -n app-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/alyso/multipass_mount:/mnt/multipass_mount

multipass launch -n db-server --network Ethernet --cpus 2 --memory 2G --disk 10G \
  --cloud-init infra/cloud-init/cloud-init.yaml \
  --mount C:/Users/alyso/multipass_mount:/mnt/multipass_mount

# 2. Get new IPs
multipass list
vim infra/ansible/inventories/hosts.ini

# 3. Build JAR
mvn clean package

# 4. Run Ansible — rebuilds Docker, Java, Postgres, PgBouncer - and Deploy
cd infra
ansible-playbook ansible/playbooks/app-server.yml
ansible-playbook --ask-vault-pass ansible/playbooks/db-server.yml
# vault password: 123

# 5. Verify
ssh ubuntu@<ip>
curl http://<ip>:8080/hello
```

---

## Key takeaways

- Spring Boot bundles Tomcat inside the JAR — no external server needed
- `@RestController` replaces `web.xml` — one class, many endpoints
- `application.properties` is loaded automatically from the classpath root
- `java -jar` replaces `docker cp` and WAR deployment
- `0.0.0.0` binding = reachable from any host on the same network
