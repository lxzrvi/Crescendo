"""Redact the signing password, then surface compiler errors as Actions annotations."""
import os
from pathlib import Path
import re
import sys

log = Path(sys.argv[1])
text = log.read_text(errors="replace") if log.exists() else ""
password = os.environ.get("CRESCENDO_RELEASE_PASSWORD", "")
if password:
    text = text.replace(password, "[REDACTED]")
log.write_text(text)
lines = []
for raw in text.splitlines():
    line = raw.strip()
    if re.search(r"(^e:|\.kt:\d|^FAILURE:|^\* What went wrong|^> |[Ee]rror:)", line):
        lines.append(line)
if not lines:
    lines = text.splitlines()[-10:]
for line in lines[-20:]:
    safe = line[:1400].replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print("::error::" + safe)
