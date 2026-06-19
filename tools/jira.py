#!/usr/bin/env python3
"""Zero-dependency Jira Cloud CLI (stdlib only).

Reads credentials from the project .env:
    JIRA_SITE        e.g. yourcompany.atlassian.net  (no scheme)
    JIRA_EMAIL       account email
    JIRA_API_TOKEN   API token from id.atlassian.com

Usage:
    python tools/jira.py whoami
    python tools/jira.py project MIN
    python tools/jira.py issuetypes MIN
    python tools/jira.py search "project = MIN ORDER BY created DESC"
    python tools/jira.py get MIN-1
    python tools/jira.py create --project MIN --type Epic --summary "Foundations" --desc "..."
    python tools/jira.py create --project MIN --type Story --summary "wx.login" --parent MIN-1 --desc "..."
    python tools/jira.py req GET /rest/api/3/myself
"""
import argparse
import base64
import json
import os
import sys
import urllib.request
import urllib.error

ENV_PATH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".env")


def load_env(path=ENV_PATH):
    env = {}
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip()
    # process env overrides file
    for k in ("JIRA_SITE", "JIRA_EMAIL", "JIRA_API_TOKEN"):
        if os.environ.get(k):
            env[k] = os.environ[k]
    return env


ENV = load_env()


def _auth_header():
    email = ENV.get("JIRA_EMAIL")
    token = ENV.get("JIRA_API_TOKEN")
    if not email or not token:
        sys.exit("ERROR: JIRA_EMAIL / JIRA_API_TOKEN missing in .env")
    raw = f"{email}:{token}".encode("utf-8")
    return "Basic " + base64.b64encode(raw).decode("ascii")


def _base_url():
    site = ENV.get("JIRA_SITE")
    if not site:
        sys.exit("ERROR: JIRA_SITE missing in .env (e.g. yourcompany.atlassian.net)")
    site = site.replace("https://", "").replace("http://", "").rstrip("/")
    return f"https://{site}"


def request(method, path, body=None):
    url = path if path.startswith("http") else _base_url() + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", _auth_header())
    req.add_header("Accept", "application/json")
    if data is not None:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as resp:
            text = resp.read().decode("utf-8")
            return resp.status, (json.loads(text) if text else {})
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8")
        try:
            payload = json.loads(text)
        except Exception:
            payload = {"raw": text}
        return e.code, payload


def adf(text):
    """Convert plain text (with newlines / '- ' bullets) to a minimal ADF doc."""
    if not text:
        text = ""
    blocks = []
    bullets = []

    def flush_bullets():
        if bullets:
            blocks.append({
                "type": "bulletList",
                "content": [
                    {"type": "listItem", "content": [
                        {"type": "paragraph", "content": [{"type": "text", "text": b}]}]}
                    for b in bullets
                ],
            })
            bullets.clear()

    for line in text.split("\n"):
        s = line.strip()
        if s.startswith("- "):
            bullets.append(s[2:].strip())
        else:
            flush_bullets()
            if s:
                blocks.append({"type": "paragraph",
                               "content": [{"type": "text", "text": s}]})
            else:
                blocks.append({"type": "paragraph", "content": []})
    flush_bullets()
    if not blocks:
        blocks = [{"type": "paragraph", "content": []}]
    return {"type": "doc", "version": 1, "content": blocks}


def pp(status, payload):
    print(f"HTTP {status}")
    print(json.dumps(payload, indent=2, ensure_ascii=False))
    return 0 if 200 <= status < 300 else 1


# ---- commands ----

def cmd_whoami(a):
    return pp(*request("GET", "/rest/api/3/myself"))


def cmd_projects(a):
    return pp(*request("GET", "/rest/api/3/project/search?maxResults=100"))


def cmd_project(a):
    return pp(*request("GET", f"/rest/api/3/project/{a.key}"))


def cmd_issuetypes(a):
    st, data = request("GET", f"/rest/api/3/project/{a.key}")
    if not (200 <= st < 300):
        return pp(st, data)
    types = [{"id": t["id"], "name": t["name"], "subtask": t.get("subtask")}
             for t in data.get("issueTypes", [])]
    print(json.dumps({"project": a.key, "style": data.get("style"),
                      "issueTypes": types}, indent=2, ensure_ascii=False))
    return 0


def cmd_search(a):
    body = {"jql": a.jql, "maxResults": a.max,
            "fields": ["summary", "status", "issuetype", "parent"]}
    st, data = request("POST", "/rest/api/3/search/jql", body)
    if 200 <= st < 300 and "issues" in data:
        for it in data["issues"]:
            f = it["fields"]
            parent = f.get("parent", {}).get("key", "") if f.get("parent") else ""
            print(f"{it['key']:10} {f['issuetype']['name']:10} "
                  f"{f['status']['name']:14} {('^'+parent) if parent else '':10} "
                  f"{f['summary']}")
        print(f"\n{data.get('total', len(data['issues']))} issue(s)")
        return 0
    return pp(st, data)


def cmd_get(a):
    return pp(*request("GET", f"/rest/api/3/issue/{a.key}"))


def cmd_create(a):
    fields = {
        "project": {"key": a.project},
        "summary": a.summary,
        "issuetype": {"name": a.type},
    }
    if a.desc:
        fields["description"] = adf(a.desc)
    if a.parent:
        fields["parent"] = {"key": a.parent}
    if a.labels:
        fields["labels"] = a.labels.split(",")
    st, data = request("POST", "/rest/api/3/issue", {"fields": fields})
    if 200 <= st < 300:
        print(f"CREATED {data['key']}  ({a.type}: {a.summary})")
        return 0
    return pp(st, data)


def cmd_update(a):
    fields = {}
    if a.summary:
        fields["summary"] = a.summary
    if a.desc is not None:
        fields["description"] = adf(a.desc)
    if a.labels:
        fields["labels"] = a.labels.split(",")
    if not fields:
        sys.exit("ERROR: nothing to update (pass --summary/--desc/--labels)")
    st, data = request("PUT", f"/rest/api/3/issue/{a.key}", {"fields": fields})
    if 200 <= st < 300:
        print(f"UPDATED {a.key}")
        return 0
    return pp(st, data)


def cmd_delete(a):
    st, data = request("DELETE", f"/rest/api/3/issue/{a.key}")
    if 200 <= st < 300:
        print(f"DELETED {a.key}")
        return 0
    return pp(st, data)


def cmd_req(a):
    body = json.loads(a.body) if a.body else None
    return pp(*request(a.method.upper(), a.path, body))


def main():
    p = argparse.ArgumentParser(description="Zero-dep Jira Cloud CLI")
    sub = p.add_subparsers(dest="cmd", required=True)

    sub.add_parser("whoami").set_defaults(fn=cmd_whoami)
    sub.add_parser("projects").set_defaults(fn=cmd_projects)

    sp = sub.add_parser("project"); sp.add_argument("key"); sp.set_defaults(fn=cmd_project)
    sp = sub.add_parser("issuetypes"); sp.add_argument("key"); sp.set_defaults(fn=cmd_issuetypes)

    sp = sub.add_parser("search"); sp.add_argument("jql")
    sp.add_argument("--max", type=int, default=50); sp.set_defaults(fn=cmd_search)

    sp = sub.add_parser("get"); sp.add_argument("key"); sp.set_defaults(fn=cmd_get)

    sp = sub.add_parser("create")
    sp.add_argument("--project", required=True)
    sp.add_argument("--type", required=True)
    sp.add_argument("--summary", required=True)
    sp.add_argument("--desc", default="")
    sp.add_argument("--parent", default="")
    sp.add_argument("--labels", default="")
    sp.set_defaults(fn=cmd_create)

    sp = sub.add_parser("update")
    sp.add_argument("key")
    sp.add_argument("--summary", default="")
    sp.add_argument("--desc", default=None)
    sp.add_argument("--labels", default="")
    sp.set_defaults(fn=cmd_update)

    sp = sub.add_parser("delete"); sp.add_argument("key"); sp.set_defaults(fn=cmd_delete)

    sp = sub.add_parser("req")
    sp.add_argument("method"); sp.add_argument("path"); sp.add_argument("body", nargs="?")
    sp.set_defaults(fn=cmd_req)

    a = p.parse_args()
    sys.exit(a.fn(a))


if __name__ == "__main__":
    main()
