#!/usr/bin/env python3
"""Generate the Android and legacy icons from one vector mark.

Requires Python 3, Pillow and Inkscape. Run from any directory.
"""
from pathlib import Path
import subprocess
import tempfile
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
BRAND = ROOT / "docs/branding"
ANDROID = "http://schemas.android.com/apk/res/android"
PATHS = [
    ('M57,76H45C37,76 32,71 32,63V43C32,35 37,30 45,30H59C67,30 72,35 72,43V54',
     'none', '#FFFFFF', '5.5'),
    ('M43,45H60 M43,56H53', 'none', '#D8D5FF', '4'),
    ('M69,59V77 M60,68H78', 'none', '#D8D5FF', '5.5'),
]


def vector(monochrome=False, compact=False):
    size = '24' if compact else '108'
    text = [f'<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="{ANDROID}"',
            f'    android:width="{size}dp" android:height="{size}dp"',
            f'    android:viewportWidth="{size}" android:viewportHeight="{size}">']
    if compact:
        text.append('    <group android:scaleX="0.4" android:scaleY="0.4" android:translateX="-10" android:translateY="-9">')
    for path, fill, stroke, width in PATHS:
        text.append(f'    <path android:pathData="{path}" android:fillColor="#00000000"'
                    f' android:strokeColor="{"#FFFFFF" if monochrome else stroke}"'
                    f' android:strokeWidth="{width}" android:strokeLineCap="round" android:strokeLineJoin="round" />')
    if compact:
        text.append('    </group>')
    return '\n'.join(text + ['</vector>', ''])


def svg():
    paths = ''.join(f'<path d="{d}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" '
                    'stroke-linecap="round" stroke-linejoin="round"/>'
                    for d, fill, stroke, width in PATHS)
    return ('<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">'
            '<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">'
            '<stop stop-color="#6860CF"/><stop offset="1" stop-color="#302567"/>'
            '</linearGradient></defs><path fill="url(#bg)" d="M0,0H108V108H0Z"/>'
            + paths + '</svg>\n')


def generate():
    BRAND.mkdir(parents=True, exist_ok=True)
    (RES / 'drawable/ic_launcher_foreground.xml').write_text(vector())
    (RES / 'drawable/ic_launcher_monochrome.xml').write_text(vector(monochrome=True))
    (RES / 'drawable/ic_noteplus.xml').write_text(vector(compact=True))
    (RES / 'drawable/ic_launcher_background.xml').write_text(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="{ANDROID}" xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0H108V108H0Z">
        <aapt:attr name="android:fillColor">
            <gradient android:startX="0" android:startY="0" android:endX="108" android:endY="108" android:type="linear">
                <item android:offset="0" android:color="#6860CF" />
                <item android:offset="1" android:color="#302567" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
''')
    # API 26 gets the original adaptive fields; Android 13 gets a dedicated themed mark.
    for qualifier in ('v26', 'v33'):
        folder = RES / f'mipmap-anydpi-{qualifier}'
        folder.mkdir(exist_ok=True)
        mono = '\n    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />' if qualifier == 'v33' else ''
        adaptive = f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="{ANDROID}">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />{mono}
</adaptive-icon>
'''
        for name in ('ic_launcher', 'ic_launcher_round'):
            (folder / f'{name}.xml').write_text(adaptive)

    source = BRAND / 'omni-noteplus.svg'
    source.write_text(svg())
    with tempfile.TemporaryDirectory() as temp:
        rendered = Path(temp) / 'icon.png'
        subprocess.run(['inkscape', str(source), '--export-type=png', f'--export-filename={rendered}',
                        '--export-width=1024', '--export-height=1024'], check=True)
        image = Image.open(rendered).convert('RGBA')
        # Legacy icons use the central adaptive viewport; all marks fit the circular safe zone.
        image = image.crop((171, 171, 853, 853))
        for density, size in (('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)):
            for name, circle in (('ic_launcher', False), ('ic_launcher_round', True)):
                icon = image.resize((size * 4, size * 4), Image.Resampling.LANCZOS)
                mask = Image.new('L', icon.size)
                draw = ImageDraw.Draw(mask)
                if circle:
                    draw.ellipse((0, 0, icon.width - 1, icon.height - 1), fill=255)
                else:
                    draw.rounded_rectangle((0, 0, icon.width - 1, icon.height - 1), radius=icon.width * 0.23, fill=255)
                icon.putalpha(mask)
                icon.resize((size, size), Image.Resampling.LANCZOS).save(
                    RES / f'mipmap-{density}/{name}.webp', 'WEBP', lossless=True)


if __name__ == '__main__':
    generate()
