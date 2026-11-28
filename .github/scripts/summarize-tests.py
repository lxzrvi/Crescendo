"""Make the CI log explicit about the unit tests that actually executed."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

reports = sorted(Path(sys.argv[1]).glob("TEST-*.xml"))
if not reports:
    raise SystemExit("::error::No unit-test reports were produced.")

tests = failures = errors = skipped = 0
for report in reports:
    suite = ET.parse(report).getroot()
    tests += int(suite.get("tests", "0"))
    failures += int(suite.get("failures", "0"))
    errors += int(suite.get("errors", "0"))
    skipped += int(suite.get("skipped", "0"))
print(f"Unit tests: {tests} total, {failures} failed, {errors} errors, {skipped} skipped.")
if tests == 0 or failures or errors:
    raise SystemExit("::error::Unit tests did not pass.")
