#!/usr/bin/env python3
"""Validate a Release AAB and compare its identity/version to Gradle."""
from pathlib import Path
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

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

# Check the built AAB, rather than assuming the Gradle setting recovered symbols.
# Pre-stripped third-party libraries cannot be repaired by debugSymbolLevel.
prefix = 'BUNDLE-METADATA/com.android.tools.build.debugsymbols/'
with zipfile.ZipFile(bundle) as archive:
    native_libraries = {
        '/'.join(name.split('/')[-2:])
        for name in archive.namelist()
        if re.fullmatch(r'[^/]+/lib/[^/]+/[^/]+\.so', name)
    }
    symbols = {
        name[len(prefix):].removesuffix('.sym').removesuffix('.dbg')
        for name in archive.namelist()
        if name.startswith(prefix) and name.endswith(('.so.sym', '.so.dbg'))
        and archive.getinfo(name).file_size > 0
    }
missing = sorted(native_libraries - symbols)
report = f'Native symbol coverage: {len(native_libraries - set(missing))}/{len(native_libraries)} libraries'
print(report)
for library in missing:
    print(f'::warning::No native debug symbols in AAB for {library}. '
          'A pre-stripped dependency needs matching unstripped binaries/symbols from its publisher.')
if summary := os.environ.get('GITHUB_STEP_SUMMARY'):
    with Path(summary).open('a') as output:
        output.write(f'\n### Native debug symbols\n\n{report}\n')
        for library in missing:
            output.write(f'- Missing: `{library}` (Play Console may still warn).\n')
