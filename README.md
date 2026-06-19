# Tour-Guide Mini App (景区定制讲解服务预约管理小程序)

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

- **Backend:** Spring Boot 3 (Java 21) + MySQL 8 + Redis, packaged with Docker Compose.
- **Admin web:** Vue 3 + TypeScript + Element Plus (Vite).
- **Mini Program:** WeChat native + TypeScript (one app, tourist/guide role switch).
- **API contract:** OpenAPI → generated TypeScript types shared by both frontends.

## Repository layout

```
.
├── backend/              # Spring Boot 3 service (Java 21, Gradle)        — see MIN-10
├── frontend/
│   ├── miniprogram/      # WeChat Mini Program (TypeScript) — tourist + guide
│   └── admin/            # Vue 3 + TS + Element Plus admin console        — see MIN-15
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
4. **Mini Program:** open `frontend/` in WeChat DevTools (root contains `project.config.json`).

## Project management

Work is tracked on the Jira board **MIN** (`Miniapp`). Phase-1 epics: MIN-1..MIN-8.
Use `tools/jira.py` to interact with the board — see [tools/README.md](tools/README.md).

## Conventions

- **Line endings:** LF in repo (enforced via `.gitattributes` / `.editorconfig`).
- **Branches:** `main` is protected; work on `feature/MIN-<id>-short-desc`, open a PR.
- **Commits:** `<type>(MIN-<id>): summary`, e.g. `feat(MIN-10): spring boot skeleton`.
  Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`.
