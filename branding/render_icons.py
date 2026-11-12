#!/usr/bin/env python3
"""Render the user-approved CDL Hub icon for Android and iOS.

The approved source is a small image received in the conversation and stored
without URL dependencies as a base64 WebP in this repository. Resampling it to
required platform sizes preserves the *approved design*, rather than inventing
a different SVG logo. For store-quality 1024 px assets, a high-resolution
original of this exact design is strongly recommended.
"""
from __future__ import annotations

import argparse
import base64
import io
from pathlib import Path

from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "branding" / "approved-cdl-hub-icon.webp.b64"


def rasterize(size: int, *, transparent: bool) -> Image.Image:
    payload = base64.b64decode(SOURCE.read_text().strip(), validate=True)
    icon = Image.open(io.BytesIO(payload)).convert("RGBA")
    if icon.size != (83, 81):
        raise ValueError("Unexpected approved-icon source dimensions")
    scaled = ImageOps.contain(icon, (size, size), Image.Resampling.LANCZOS)
    if transparent:
        canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        canvas.paste(scaled, ((size - scaled.width)//2, (size - scaled.height)//2), scaled)
    else:
        # Apple app icons must be opaque, including transparent source corners.
        canvas = Image.new("RGB", (size, size), "#07162b")
        canvas.paste(scaled, ((size - scaled.width)//2, (size - scaled.height)//2), scaled)
    return canvas


def render_android() -> None:
    directory = ROOT / "android/app/src/main/res/drawable"
    directory.mkdir(parents=True, exist_ok=True)
    # Android must use the approved PNG, not the older SVG vector fallback.
    (directory / "cdl_hub_launcher.xml").unlink(missing_ok=True)
    target = directory / "cdl_hub_launcher.png"
    rasterize(512, transparent=True).save(target, optimize=True)
    print(f"Android approved launcher: {target} ({target.stat().st_size} bytes)")


def render_ios() -> None:
    directory = ROOT / "ios/CDLDriverHub/CDLDriverHub/Assets.xcassets/AppIcon.appiconset"
    directory.mkdir(parents=True, exist_ok=True)
    for size, name in [
        (120, "Icon-60@2x.png"),
        (180, "Icon-60@3x.png"),
        (1024, "AppIcon-1024.png"),
    ]:
        target = directory / name
        rasterize(size, transparent=False).save(target, optimize=True)
        print(f"iOS approved launcher: {target} ({target.stat().st_size} bytes)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Render approved CDL Hub icons")
    parser.add_argument("--platform", choices=("android","ios","all"), default="all")
    args = parser.parse_args()
    if args.platform in ("android", "all"):
        render_android()
    if args.platform in ("ios", "all"):
        render_ios()
