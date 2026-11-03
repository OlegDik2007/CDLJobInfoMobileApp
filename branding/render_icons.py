#!/usr/bin/env python3
"""Build Android and iOS launcher icons from one version-controlled SVG brand source.

Run before a release build:
  python3 -m pip install cairosvg Pillow
  python3 branding/render_icons.py --platform android
  python3 branding/render_icons.py --platform ios

The Android XML icon is a local-build fallback. CI replaces it with a rendered PNG
in its ephemeral checkout; no binary GitHub upload is required.
"""
from __future__ import annotations

import argparse
import io
from pathlib import Path

import cairosvg
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "branding" / "cdl-hub-icon.svg"


def rasterize(size: int, *, transparent: bool) -> Image.Image:
    raw = cairosvg.svg2png(
        url=str(SOURCE),
        output_width=size,
        output_height=size,
    )
    image = Image.open(io.BytesIO(raw)).convert("RGBA")
    if transparent:
        return image
    # Apple rejects alpha channels in marketing App Store icons.
    background = Image.new("RGB", image.size, "#07162b")
    background.paste(image, (0, 0), image.getchannel("A"))
    return background


def render_android() -> None:
    directory = ROOT / "android" / "app" / "src" / "main" / "res" / "drawable"
    directory.mkdir(parents=True, exist_ok=True)
    fallback_vector = directory / "cdl_hub_launcher.xml"
    fallback_vector.unlink(missing_ok=True)
    output = directory / "cdl_hub_launcher.png"
    rasterize(512, transparent=True).save(output, optimize=True)
    print(f"Android: {output} ({output.stat().st_size:,} bytes)")


def render_ios() -> None:
    directory = (
        ROOT / "ios" / "CDLDriverHub" / "CDLDriverHub"
        / "Assets.xcassets" / "AppIcon.appiconset"
    )
    directory.mkdir(parents=True, exist_ok=True)
    for size, name in [
        (120, "Icon-60@2x.png"),
        (180, "Icon-60@3x.png"),
        (1024, "AppIcon-1024.png"),
    ]:
        output = directory / name
        rasterize(size, transparent=False).save(output, optimize=True)
        print(f"iOS: {output} ({output.stat().st_size:,} bytes)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Render CDL Hub icons from SVG")
    parser.add_argument(
        "--platform",
        choices=("android", "ios", "all"),
        default="all",
    )
    args = parser.parse_args()
    if args.platform in ("android", "all"):
        render_android()
    if args.platform in ("ios", "all"):
        render_ios()
