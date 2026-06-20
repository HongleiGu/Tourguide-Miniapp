# Tour-Guide Mini App (景区定制讲解服务预约管理小程序)

[![CI](https://github.com/HongleiGu/Tourguide-Miniapp/actions/workflows/ci.yml/badge.svg)](https://github.com/HongleiGu/Tourguide-Miniapp/actions/workflows/ci.yml)

A scenic-area guide-booking platform: tourists book guided tours (private / group-buy / time-slot),
guides accept and verify orders, and scenic-area admins manage staff, sessions, pricing, dispatch,
and reporting.

Three roles across two surfaces:

| Role | Surface |
|------|---------|
| 游客 (Tourist) | WeChat Mini Program |
| 讲解员 (Guide) | WeChat Mini Program (same app, role switch) |
| 景区管理员 (Admin) | PC web (browser) |

## Tech stack

- **Backend:** Spring Boot 3.5 (Java 21, Maven) + MySQL 8 + Redis, packaged with Docker Compose.
- **Admin web:** Vue 3 + TypeScript + Element Plus (Vite).
- **Mini Programs:** two separate WeChat apps (TypeScript) — **tourist** (游客) and **guide** (讲解员) — sharing runtime utils via a sync script.
- **API contract:** OpenAPI → generated TypeScript types shared by the frontends.

## Repository layout

```
.
├── backend/              # Spring Boot 3.5 service (Java 21, Maven)        — see MIN-10
├── frontend/
│   ├── admin/            # Vue 3 + TS + Element Plus admin console        — see MIN-15
│   ├── tourist-miniapp/  # WeChat Mini Program (TS) — 游客
│   ├── guide-miniapp/    # WeChat Mini Program (TS) — 讲解员
│   └── shared/           # api.d.ts (OpenAPI types) + miniapp/ shared utils — see MIN-14
├── docker-compose.yml    # Full local stack: mysql + redis + app + nginx  — see MIN-16
├── tools/
│   └── jira.py           # Zero-dependency Jira CLI (project board: MIN)
├── .env.example          # Template for local secrets — copy to .env
├── 需求梳理-0614.docx     # Source requirements document
└── _requirements.txt     # Extracted plain-text requirements
```

## Prerequisites

- JDK 21, Docker + Docker Compose
- Node.js 20+ (admin web)
- WeChat DevTools (mini program)
- Python 3.11+ (only for `tools/jira.py`)

## Getting started

1. **Secrets:** `cp .env.example .env` and fill in real values. **Never commit `.env`.**
2. **Backend:** see `backend/` (MIN-10). Local stack via `docker compose up` (MIN-16).
3. **Admin web:** see `frontend/admin/` (MIN-15).
4. **Mini Programs:** run `node tools/sync-miniapp-shared.mjs` first (copies shared utils into each app),
   then open `frontend/tourist-miniapp/` or `frontend/guide-miniapp/` in WeChat DevTools.
   Each needs its own AppID; the guide app's is a placeholder until registered.

### Full stack via Docker Compose (MIN-16)

```bash
docker compose up -d --build          # mysql + redis + app + nginx
```

Then open **http://localhost** — nginx serves the admin SPA and reverse-proxies `/api/*`
to the backend. No domain or TLS needed locally (`server_name _`). Individual services:

```bash
docker compose up -d mysql redis      # just the datastores (for host-run backend/admin dev)
```

A domain + HTTPS + ICP 备案 are required later for WeChat (Pay callbacks, mini-program
request domains) — see `frontend/admin/nginx.conf` for where to add `server_name` + TLS.

## Project management

Work is tracked on the Jira board **MIN** (`Miniapp`). Phase-1 epics: MIN-1..MIN-8.
Use `tools/jira.py` to interact with the board — see [tools/README.md](tools/README.md).

## Conventions

- **Line endings:** LF in repo (enforced via `.gitattributes` / `.editorconfig`).
- **Branches:** `main` is protected; work on `feature/MIN-<id>-short-desc`, open a PR.
- **Commits:** `<type>(MIN-<id>): summary`, e.g. `feat(MIN-10): spring boot skeleton`.
  Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`.
