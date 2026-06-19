# tools/jira.py — Zero-dependency Jira CLI

Stdlib-only (urllib) Jira Cloud CLI. Reads credentials from the repo `.env`:
`JIRA_SITE`, `JIRA_EMAIL`, `JIRA_API_TOKEN`. Board: **MIN**.

## Running (Windows + conda)

Call the conda `study` env interpreter **directly** (not via `conda run`, which
rejects arguments containing newlines), and force UTF-8 so Chinese renders:

```bash
PY="D:/anaconda3/envs/study/python.exe"
PYTHONIOENCODING=utf-8 PYTHONUTF8=1 "$PY" tools/jira.py whoami
```

## Commands

| Command | Purpose |
|---------|---------|
| `whoami` | Verify auth, show account |
| `projects` | List projects |
| `project MIN` | Project details |
| `issuetypes MIN` | Issue types + project style |
| `search "<JQL>"` | Search issues (quote `"MIN"` — it's a JQL reserved word) |
| `get MIN-1` | Full issue JSON |
| `create --project MIN --type <type> --summary S [--desc D] [--parent K] [--labels a,b]` | Create issue |
| `update MIN-1 [--summary S] [--desc D] [--labels a,b]` | Update fields |
| `delete MIN-1` | Delete issue |
| `req GET /rest/api/3/...` | Raw REST escape hatch |

Notes:
- Epic issue type in this board is `长篇故事` (zh-CN name). Child link via `--parent`.
- Descriptions are converted to Atlassian Document Format; `- ` lines become bullets.
