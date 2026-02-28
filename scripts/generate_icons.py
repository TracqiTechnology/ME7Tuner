#!/usr/bin/env python3
"""Generate launcher icons for native distributions (macOS .icns, Windows .ico, Linux .png).

Uses pistons_0.png tinted dark on a gold background.
"""

import subprocess
import tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ICON_PATH = ROOT / "src" / "main" / "resources" / "pistons_0.png"
OUT_DIR = ROOT / "src" / "main" / "resources" / "icons"

GOLD_BG = (249, 185, 37, 255)       # #F9B925 — PrimaryContainer
DARK_TINT = (65, 45, 0)             # #412D00 — OnPrimary (dark warm brown)

CANVAS_SIZE = 1024  # master canvas size
MARGIN_FRAC = 0.05  # 5% padding on each side


def load_and_tint(path: Path, tint_rgb: tuple) -> Image.Image:
    """Load a RGBA image and tint white pixels to tint_rgb using luminance."""
    icon = Image.open(path).convert("RGBA")
    arr = np.array(icon)
    alpha = arr[:, :, 3]
    lum = arr[:, :, :3].max(axis=2).astype(float) / 255.0
    arr[:, :, 0] = (lum * tint_rgb[0]).astype(np.uint8)
    arr[:, :, 1] = (lum * tint_rgb[1]).astype(np.uint8)
    arr[:, :, 2] = (lum * tint_rgb[2]).astype(np.uint8)
    arr[:, :, 3] = alpha
    return Image.fromarray(arr)


def make_rounded_mask(size: int, radius: int) -> Image.Image:
    """Create a rounded-rectangle alpha mask (standard macOS icon shape)."""
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), (size - 1, size - 1)], radius=radius, fill=255)
    return mask


def build_master_icon() -> Image.Image:
    """Build the master 1024x1024 icon image."""
    piston = load_and_tint(ICON_PATH, DARK_TINT)

    # Scale piston to fit within the canvas minus margin
    inner = int(CANVAS_SIZE * (1 - 2 * MARGIN_FRAC))
    scale = min(inner / piston.width, inner / piston.height)
    new_w = int(piston.width * scale)
    new_h = int(piston.height * scale)
    piston = piston.resize((new_w, new_h), Image.LANCZOS)

    # Gold background
    img = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), GOLD_BG)

    # Center the piston
    x = (CANVAS_SIZE - new_w) // 2
    y = (CANVAS_SIZE - new_h) // 2
    img.paste(piston, (x, y), piston)

    return img


def apply_macos_mask(img: Image.Image) -> Image.Image:
    """Apply rounded-corner mask (macOS icon standard: ~22.37% corner radius)."""
    size = img.width
    radius = int(size * 0.2237)
    mask = make_rounded_mask(size, radius)
    result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    result.paste(img, (0, 0), mask)
    return result


def generate_icns(master: Image.Image, out_path: Path):
    """Generate .icns via iconutil (macOS only)."""
    rounded = apply_macos_mask(master)

    with tempfile.TemporaryDirectory() as tmpdir:
        iconset = Path(tmpdir) / "icon.iconset"
        iconset.mkdir()

        sizes = [16, 32, 128, 256, 512]
        for s in sizes:
            # 1x
            resized = rounded.resize((s, s), Image.LANCZOS)
            resized.save(iconset / f"icon_{s}x{s}.png", "PNG")
            # 2x
            resized2x = rounded.resize((s * 2, s * 2), Image.LANCZOS)
            resized2x.save(iconset / f"icon_{s}x{s}@2x.png", "PNG")

        subprocess.run(
            ["iconutil", "-c", "icns", str(iconset), "-o", str(out_path)],
            check=True,
        )
    print(f"  .icns -> {out_path}")


def generate_ico(master: Image.Image, out_path: Path):
    """Generate .ico with standard Windows sizes."""
    sizes = [(16, 16), (32, 32), (48, 48), (256, 256)]
    master.save(out_path, format="ICO", sizes=sizes)
    print(f"  .ico  -> {out_path}")


def generate_png(master: Image.Image, out_path: Path):
    """Generate 512x512 PNG for Linux."""
    resized = master.resize((512, 512), Image.LANCZOS)
    resized.save(out_path, "PNG")
    print(f"  .png  -> {out_path}")


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    print("Building master icon (1024x1024)...")
    master = build_master_icon()

    print("Generating icons:")
    generate_png(master, OUT_DIR / "icon.png")
    generate_ico(master, OUT_DIR / "icon.ico")

    # .icns requires macOS iconutil
    import shutil
    if shutil.which("iconutil"):
        generate_icns(master, OUT_DIR / "icon.icns")
    else:
        print("  .icns -> SKIPPED (iconutil not available; macOS only)")

    print("Done.")


if __name__ == "__main__":
    main()
