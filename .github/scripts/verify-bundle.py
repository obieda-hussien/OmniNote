#!/usr/bin/env python3
"""Validate a Release AAB and compare its identity/version to Gradle."""
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
bundles = list(Path(sys.argv[1]).glob('*.aab'))
if len(bundles) != 1:
    raise SystemExit(f'Expected one AAB in {sys.argv[1]}, found {len(bundles)}')
bundle = bundles[0]
bundletool = ['java', '-jar', sys.argv[2]]
subprocess.run(bundletool + ['validate', f'--bundle={bundle}'], check=True)
manifest = ET.fromstring(subprocess.check_output(
    bundletool + ['dump', 'manifest', f'--bundle={bundle}', '--module=base'], text=True))
android = '{http://schemas.android.com/apk/res/android}'
for key, value in expected.items():
    actual = manifest.get(key if key == 'package' else android + key)
    if actual != value:
        raise SystemExit(f'AAB {key}: expected {value!r}, found {actual!r}')
print(f'{bundle.name}: {expected}, {bundle.stat().st_size / 1024 / 1024:.2f} MiB')
