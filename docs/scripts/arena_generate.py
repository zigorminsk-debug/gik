"""Call an official Arena API endpoint from Actions.

Do not automate arena.ai's web login or put a password in this workflow.
The endpoint and token are repository secrets so the integration can follow
Arena's current documented API without exposing credentials in the APK.
"""
import json
import os
import urllib.request

url = os.environ.get("ARENA_API_URL", "").rstrip("/")
key = os.environ.get("ARENA_API_KEY", "")
if not url or not key:
    raise SystemExit("Set ARENA_API_URL and ARENA_API_KEY in repository Actions secrets")

with open(".gik/task.txt", encoding="utf-8") as f:
    task = f.read().strip()

# Expected contract for the adapter: POST /generate returns generated files
# as {"files": [{"path": "...", "content": "..."}]}. Adapt this small
# boundary if Arena publishes a different official API schema.
payload = json.dumps({"prompt": task, "output": "android-project"}).encode()
request = urllib.request.Request(
    url + "/generate", data=payload,
    headers={"Authorization": "Bearer " + key, "Content-Type": "application/json"},
)
with urllib.request.urlopen(request, timeout=300) as response:
    result = json.load(response)

files = result.get("files", [])
if not files:
    raise SystemExit("Arena returned no generated files")
for item in files:
    path = item["path"]
    if path.startswith("/") or ".." in path.split("/"):
        raise SystemExit("Refusing unsafe generated path: " + path)
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(item["content"])
print(f"Generated {len(files)} files")
