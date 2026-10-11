#!/usr/bin/env python3
"""Check that the compiled APK carries the identity/version declared by Gradle."""
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

build = Path('app/build.gradle.kts').read_text()
expected = {
    'package': re.search(r'applicationId\s*=\s*"([^"]+)"', build)[1],
    'versionCode': re.search(r'versionCode\s*=\s*(\d+)', build)[1],
    'versionName': re.search(r'versionName\s*=\s*"([^"]+)"', build)[1],
}
apks = list(Path(sys.argv[1]).glob('*.apk'))
if len(apks) != 1:
    raise SystemExit(f'Expected one APK in {sys.argv[1]}, found {len(apks)}')
apk = apks[0]
manifest = ET.fromstring(subprocess.check_output(['apkanalyzer', 'manifest', 'print', str(apk)], text=True))
android = '{http://schemas.android.com/apk/res/android}'
for key, value in expected.items():
    actual = manifest.get(key if key == 'package' else android + key)
    if actual != value:
        raise SystemExit(f'APK {key}: expected {value!r}, found {actual!r}')
print(f'{apk.name}: {expected}, {apk.stat().st_size / 1024 / 1024:.2f} MiB')
