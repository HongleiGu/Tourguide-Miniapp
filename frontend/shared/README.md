# Shared API types

Generated TypeScript types for the backend HTTP API, consumed by both the **miniprogram**
(`frontend/`) and the **admin** (`frontend/admin/`, MIN-15).

- `openapi.json` — snapshot of the backend OpenAPI spec
- `api.d.ts` — types generated from it by [openapi-typescript](https://github.com/openapi-ts/openapi-typescript)

Both files are committed so the frontends build without running the backend. **Do not edit
`api.d.ts` by hand** — it is regenerated.

## Regenerate

```bash
# from a running dev backend (default http://localhost:8080/v3/api-docs)
tools/gen-api-types.sh

# or offline, from the committed snapshot
tools/gen-api-types.sh frontend/shared/openapi.json
```

## Usage

```ts
import type { components, paths } from "../shared/api"; // adjust the relative path

type PingResponse = components["schemas"]["PingResponse"];
type PingGet = paths["/api/ping"]["get"];
```
