# Admin — Vue 3 + TypeScript + Element Plus

PC management console for the tour-guide platform (景区管理后台). Scaffolded in **MIN-15**.

Stack: Vite + Vue 3 + TypeScript + Element Plus + Vue Router + Pinia + Axios.

## Run

```bash
npm install
npm run dev        # http://localhost:5173  (proxies /api -> http://localhost:8080)
npm run build      # vue-tsc typecheck + production bundle
```

For the dashboard's backend-connectivity card to show "在线", run the backend
(`cd ../../backend && ./mvnw spring-boot:run`) with MySQL + Redis up
(`docker compose up -d mysql redis` from the repo root).

## Layout

- `src/router/` — routes + auth guard (redirects to `/login` when unauthenticated)
- `src/stores/` — Pinia stores (`auth` — dev stub; real login lands in MIN-2)
- `src/api/` — Axios instance + interceptors (`http.ts`), typed calls (`ping.ts`),
  and `types.ts` which re-exports the **shared** API types from `frontend/shared/api.d.ts`
  (alias `@shared`, generated in MIN-14)
- `src/layouts/MainLayout.vue` — sidebar + header shell
- `src/views/` — `Login.vue`, `Dashboard.vue`

Path aliases: `@` → `src`, `@shared` → `../shared`.
