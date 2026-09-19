# Hive

**A real-time collaborative workspace for developers.**

Hive is a real-time collaborative coding platform. Multiple developers can edit code together with conflict-free CRDT synchronization and live cursors, communicate in a room chat, and save code snapshots — with role-based permissions (Owner, Editor, Viewer) enforced across REST endpoints and WebSocket channels.

---

## Features

| Area | What it does |
| --- | --- |
| **Auth** | Register / login, JWT access tokens, database-backed revocable refresh tokens with rotation, BCrypt hashing |
| **Rooms** | Create a room (creator becomes `OWNER`), join by invite code, list your rooms, soft delete |
| **Roles** | `OWNER` manages members and the room; `EDITOR` edits, runs and chats; `VIEWER` watches and chats |
| **Collaborative editor** | Monaco Editor + Yjs CRDT relayed over STOMP, with a peer sync handshake so late joiners converge |
| **Presence** | Live per-room member list and remote cursors, synchronized across multiple tabs per user |
| **Chat** | Per-room real-time chat over WebSocket, persisted to MySQL, cursor-paginated history |
| **Save / load** | Manual save plus debounced autosave into versioned `CodeSnapshot` records; latest loads on join |
| **Extensible Execution** | Pluggable `LanguageExecutor` strategy pattern designed to support multiple programming languages |

---

## Architecture

```
frontend/  React 19 + Vite + Tailwind v4 + Monaco + Yjs        (:5173)
backend/   Spring Boot 3.5 + Spring Security + STOMP + JPA     (:8080)
MySQL      MySQL 8.0+                                          (:3306)
```

**Security boundaries worth calling out:**

- The STOMP `CONNECT` frame is authenticated by its own JWT interceptor; the HTTP handshake alone is never trusted. Every `SEND` and `SUBSCRIBE` is then re-authorized against the room's role table.
- Message handlers re-check permissions themselves rather than relying on the interceptor alone.
- Refresh tokens are stored only as SHA-256 hashes and are rotated (and revoked) on every use.
- No secrets are hardcoded: credentials and configuration are externalized via environment variables.

---

## Prerequisites

- **Java 21+**
- **Node.js 20+**
- **MySQL 8.0+**

---

## Running the stack locally

### 1. Database Setup

Ensure MySQL is running on `localhost:3306` with database `hive`. Flyway database migrations run automatically on startup.

Default credentials configured in `backend/src/main/resources/application.yml`:
- Database: `hive`
- Username: `hive` (or override via `DB_USERNAME`)
- Password: `hive_local` (or override via `DB_PASSWORD`)

### 2. Start the backend

```bash
cd backend
./mvnw spring-boot:run
```
*(On Windows: `.\mvnw.cmd spring-boot:run` or run `HiveApplication` directly from your IDE)*

The API comes up on http://localhost:8080.

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173, register an account, and create a room.

To try collaboration, open the room, copy the invite code from **Invite**, then join from a second
browser profile or an incognito window with a different account.

---

## Configuration

Every value below is read from the environment, with a local-development default in
`backend/src/main/resources/application.yml`.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/hive?...` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `hive` / `hive_local` | Database credentials |
| `JWT_SECRET` | *(empty — must be set)* | HMAC signing key, ≥32 bytes |
| `JWT_ISSUER` | `hive-api` | Token issuer claim |
| `JWT_ACCESS_TOKEN_TTL` | `15m` | Access-token lifetime |
| `JWT_REFRESH_TOKEN_TTL` | `30d` | Refresh-token lifetime |
| `WS_ALLOWED_ORIGINS` | `http://localhost:5173` | Allowed WebSocket origins |
| `DOCKER_JAVA_IMAGE` | `openjdk:21-slim` | Sandbox image |
| `DOCKER_COMMAND` | `docker` | Docker CLI path |
| `EXECUTION_TIMEOUT` | `10s` | Hard wall-clock limit per run |
| `EXECUTION_MEMORY_LIMIT` | `128m` | Container memory cap |
| `EXECUTION_CPU_LIMIT` | `0.5` | Container CPU cap |
| `EXECUTION_MAX_CONCURRENT` | `2` | Containers running at once, server-wide |
| `EXECUTION_RATE_LIMIT_PER_MINUTE` | `10` | Runs per room per minute |

The frontend reads `VITE_API_BASE_URL` (default `http://localhost:8080`).

---

## API reference

### Auth
| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/api/auth/register` | `{username, email, password}` → token pair |
| `POST` | `/api/auth/login` | `{identifier, password}` — identifier is username or email |
| `POST` | `/api/auth/refresh` | `{refreshToken}` → new pair; the old token is revoked |

### Users
| Method | Path | Notes |
| --- | --- | --- |
| `GET` | `/api/users/me` | Current profile |
| `PUT` | `/api/users/me` | Update username / email / avatar |

### Rooms
| Method | Path | Role |
| --- | --- | --- |
| `POST` | `/api/rooms` | any |
| `POST` | `/api/rooms/join` | any |
| `GET` | `/api/rooms` | any |
| `GET` | `/api/rooms/{id}` | member |
| `GET` | `/api/rooms/{id}/members` | member |
| `PATCH` | `/api/rooms/{id}/members/{userId}/role` | owner |
| `DELETE` | `/api/rooms/{id}/members/{userId}` | owner |
| `DELETE` | `/api/rooms/{id}/members/me` | member (leave) |
| `DELETE` | `/api/rooms/{id}` | owner |

### Chat, code and execution
| Method | Path | Role |
| --- | --- | --- |
| `GET` | `/api/rooms/{id}/messages?size=&beforeId=` | member |
| `GET` | `/api/rooms/{id}/snapshots/latest` | member |
| `POST` | `/api/rooms/{id}/snapshots` | editor |
| `POST` | `/api/rooms/{id}/executions` | editor |

### WebSocket (STOMP over SockJS at `/ws`)

Authenticate by sending `Authorization: Bearer <accessToken>` as a **CONNECT frame header**.

| Destination | Direction | Role |
| --- | --- | --- |
| `/app/room/{id}/edit` | publish | editor |
| `/app/room/{id}/sync` | publish | member |
| `/app/room/{id}/chat` | publish | member |
| `/app/room/{id}/presence` | publish | member |
| `/topic/room/{id}/code` | subscribe | member |
| `/topic/room/{id}/chat` | subscribe | member |
| `/topic/room/{id}/presence` | subscribe | member |

---

## Tests

```bash
cd backend && ./mvnw test
```

Covers auth and token rotation, room permissions, chat persistence and pagination, snapshot
versioning, the STOMP authentication and authorization interceptors, presence tracking, and the
execution rate limiter. The Docker sandbox tests are guarded by `@EnabledIf("dockerAvailable")` and
skip automatically when no Docker daemon is reachable; start Docker to exercise the real resource
limits.

---

## Project layout

```
backend/src/main/java/com/hive/
  auth/       registration, login, refresh-token rotation
  user/       profile endpoints
  room/       rooms, members, RoomPermissionService
  chat/       chat persistence, REST history, STOMP handler
  code/       Yjs relay, snapshot save/load
  execution/  LanguageExecutor strategy + Docker sandbox runner
  websocket/  STOMP config, JWT + authorization interceptors, presence
  security/   JWT service, filter, SecurityConfig
frontend/src/
  pages/      Landing, Login, Register, Dashboard, Room, Profile
  components/ CollaborativeEditor, ChatPanel, MembersPanel, OutputPanel, ui
  hooks/      useAuth, useRoomSocket, usePresence, useChat, useCodeSnapshot, useExecution
  lib/        api client, STOMP client, Yjs provider
```

---

## Adding another language

`LanguageExecutor` is a strategy interface — implement it, register the bean, and add the enum
constant to `ProgrammingLanguage`. `CodeExecutionService` resolves the executor by language, so no
existing code needs to change.
