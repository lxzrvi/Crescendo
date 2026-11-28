#!/usr/bin/env python3
"""Guard the profile silhouette and launcher against geometry drifting apart."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
svg = ET.parse(root / "logo/Crescendo_main_icon.svg").getroot()
source_path = svg.find("{http://www.w3.org/2000/svg}path")
if source_path is None:
    raise SystemExit("Original launcher SVG has no mark path")
strings = ET.parse(root / "app/src/main/res/values/strings.xml").getroot()
shared = next((item.text for item in strings if item.get("name") == "launcher_mark_path"), None)
if shared is None or " ".join(shared.split()) != " ".join(source_path.get("d", "").split()):
    raise SystemExit("Shared launcher/profile path differs from the original logo")
vector = ET.parse(root / "app/src/main/res/drawable/ic_launcher_foreground.xml").getroot()
android = "{http://schemas.android.com/apk/res/android}"
vector_path = next(vector.iter("path"), None)
if vector_path is None or vector_path.get(android + "pathData") != "@string/launcher_mark_path":
    raise SystemExit("Launcher foreground is not using the shared mark")
profile = (root / "app/src/main/java/com/lxzrvi/crescendo/ui/home/ProfileMenu.kt").read_text()
if "R.string.launcher_mark_path" not in profile:
    raise SystemExit("Profile menu is not using the shared mark")
print("Launcher and profile share the original Crescendo logo path.")
