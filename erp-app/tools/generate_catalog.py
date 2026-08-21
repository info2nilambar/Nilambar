#!/usr/bin/env python3
"""Generates the product Flyway seeds and the matching placeholder PNG images.

Images are drawn locally (no third-party assets are downloaded or committed) so the catalog is
fully self-contained and reproducible: re-running this script yields byte-identical output.

Usage:
    python3 tools/generate_catalog.py
"""
from __future__ import annotations

import hashlib
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
IMAGE_DIR = ROOT / "src/main/resources/static/images/products"
MIGRATION = ROOT / "src/main/resources/db/migration/V3__seed_products.sql"
PRODUCE_MIGRATION = ROOT / "src/main/resources/db/migration/V5__seed_produce.sql"

WIDTH, HEIGHT = 600, 600

CATALOG = [
    ("Electronics", [
        ("Wireless Earbuds Pro", 4999.00, 120),
        ("Bluetooth Speaker 20W", 2499.00, 80),
        ("Smart Fitness Band", 3299.00, 150),
        ("USB-C Fast Charger 65W", 1899.00, 200),
        ("1080p Webcam", 2799.00, 60),
        ("Mechanical Keyboard", 5499.00, 45),
        ("Wireless Mouse", 1299.00, 180),
        ("Power Bank 20000mAh", 2199.00, 95),
        ("Noise Cancelling Headphones", 8999.00, 35),
        ("Smart LED Bulb", 799.00, 240),
    ]),
    ("Home & Kitchen", [
        ("Stainless Steel Pressure Cooker 5L", 2699.00, 70),
        ("Non-stick Frying Pan 24cm", 1199.00, 110),
        ("Electric Kettle 1.5L", 1499.00, 90),
        ("Insulated Water Bottle 1L", 899.00, 160),
        ("Ceramic Dinner Set 18pc", 3499.00, 40),
        ("Cotton Bed Sheet Double", 1599.00, 85),
        ("Microfibre Cleaning Cloth Pack", 349.00, 300),
        ("Storage Container Set 6pc", 1099.00, 130),
        ("Wall Clock Silent Sweep", 749.00, 100),
        ("Table Lamp Warm White", 1249.00, 75),
    ]),
    ("Grocery", [
        ("Basmati Rice 5kg", 649.00, 250),
        ("Cold Pressed Groundnut Oil 1L", 329.00, 220),
        ("Whole Wheat Atta 10kg", 499.00, 180),
        ("Toor Dal 2kg", 289.00, 210),
        ("Filter Coffee Powder 500g", 419.00, 140),
        ("Assam Tea Leaves 1kg", 559.00, 120),
        ("Raw Forest Honey 500g", 449.00, 95),
        ("Roasted Almonds 500g", 699.00, 105),
        ("Rock Salt 1kg", 89.00, 320),
        ("Turmeric Powder 500g", 179.00, 260),
    ]),
    ("Fashion", [
        ("Cotton Round Neck T-Shirt", 799.00, 190),
        ("Slim Fit Denim Jeans", 1999.00, 120),
        ("Formal Cotton Shirt", 1499.00, 140),
        ("Running Shoes", 3299.00, 75),
        ("Leather Belt", 899.00, 160),
        ("Analog Wrist Watch", 2599.00, 65),
        ("Canvas Backpack 25L", 1799.00, 88),
        ("Cotton Kurta", 1299.00, 110),
        ("Polarised Sunglasses", 1599.00, 95),
        ("Woollen Muffler", 649.00, 130),
    ]),
    ("Stationery", [
        ("A4 Notebook Pack of 5", 449.00, 280),
        ("Gel Pen Set of 10", 299.00, 340),
        ("Desk Organiser", 899.00, 90),
        ("Sticky Notes Pack", 199.00, 400),
        ("Whiteboard 2x3 ft", 1899.00, 30),
        ("Document File Folder Set", 549.00, 150),
        ("Mechanical Pencil Set", 379.00, 200),
        ("Geometry Box", 259.00, 175),
        ("Highlighter Pack of 6", 289.00, 230),
        ("A3 Drawing Sheets Pack", 399.00, 120),
    ]),
]

# Fresh produce is seeded separately in V5 so the already-applied V3 checksum stays intact.
PRODUCE_CATALOG = [
    ("Vegetables", [
        ("Tomato 1kg", 39.00, 400),
        ("Onion 1kg", 34.00, 450),
        ("Potato 1kg", 32.00, 500),
        ("Carrot 500g", 29.00, 320),
        ("Cauliflower 1pc", 45.00, 180),
        ("Green Capsicum 500g", 49.00, 220),
        ("Brinjal 500g", 27.00, 240),
        ("Lady Finger 500g", 35.00, 260),
        ("Spinach Bunch 250g", 25.00, 300),
        ("Green Peas 500g", 59.00, 200),
    ]),
    ("Fruits", [
        ("Banana Robusta 1kg", 55.00, 380),
        ("Apple Shimla 1kg", 179.00, 260),
        ("Mango Alphonso 1kg", 249.00, 150),
        ("Orange Nagpur 1kg", 119.00, 240),
        ("Papaya 1pc", 69.00, 170),
        ("Watermelon 1pc", 89.00, 130),
        ("Pomegranate 500g", 139.00, 190),
        ("Green Grapes 500g", 79.00, 210),
        ("Pineapple 1pc", 75.00, 160),
        ("Sweet Lime 1kg", 99.00, 200),
    ]),
]

PALETTE = {
    "Electronics": (33, 64, 120),
    "Home & Kitchen": (150, 74, 30),
    "Grocery": (36, 110, 60),
    "Fashion": (120, 36, 92),
    "Stationery": (90, 80, 24),
    "Vegetables": (44, 96, 40),
    "Fruits": (168, 88, 24),
}

# 5x7 bitmap font, enough for the SKU stamped on each tile.
GLYPHS = {
    "0": ("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    "1": ("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    "2": ("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    "3": ("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    "4": ("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    "5": ("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
    "6": ("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
    "7": ("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    "8": ("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    "9": ("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
    "P": ("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
    "R": ("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
    "D": ("11100", "10010", "10001", "10001", "10001", "10010", "11100"),
    "-": ("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
}


def draw_text(pixels, text, x0, y0, scale, colour):
    cursor = x0
    for char in text:
        glyph = GLYPHS.get(char)
        if glyph:
            for row, bits in enumerate(glyph):
                for col, bit in enumerate(bits):
                    if bit == "1":
                        for dy in range(scale):
                            for dx in range(scale):
                                px, py = cursor + col * scale + dx, y0 + row * scale + dy
                                if 0 <= px < WIDTH and 0 <= py < HEIGHT:
                                    pixels[py][px] = colour
        cursor += 6 * scale


def write_png(path, pixels):
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b in row:
            raw += bytes((r, g, b))

    def chunk(tag, data):
        payload = tag + data
        return struct.pack(">I", len(data)) + payload + struct.pack(">I", zlib.crc32(payload) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


def build_image(sku, name, category):
    base = PALETTE[category]
    tint = int(hashlib.sha256(name.encode()).hexdigest()[:2], 16) // 6
    top = tuple(min(255, c + 60 + tint) for c in base)
    bottom = tuple(max(0, c - 20) for c in base)

    pixels = []
    for y in range(HEIGHT):
        ratio = y / (HEIGHT - 1)
        row_colour = tuple(int(top[i] + (bottom[i] - top[i]) * ratio) for i in range(3))
        pixels.append([row_colour] * WIDTH)

    # Card panel so the label stays readable regardless of the gradient.
    panel = (245, 245, 245)
    for y in range(HEIGHT - 190, HEIGHT - 60):
        pixels[y] = list(pixels[y])
        for x in range(60, WIDTH - 60):
            pixels[y][x] = panel

    draw_text(pixels, sku, 96, HEIGHT - 160, 10, (30, 30, 30))
    return pixels


def sql_escape(value):
    return value.replace("'", "''")


def build_rows(catalog, start_index):
    rows = []
    index = start_index
    for category, products in catalog:
        for name, price, stock in products:
            index += 1
            sku = f"PRD-{index:03d}"
            image_path = f"/images/products/{sku}.png"
            write_png(IMAGE_DIR / f"{sku}.png", build_image(sku, name, category))
            description = (
                f"{name} from the {category} range. Quality checked, warehouse fresh and eligible for "
                f"same-day home delivery inside the service radius."
            )
            rows.append(
                "  ('{sku}', '{name}', '{description}', '{category}', {price:.2f}, {stock}, '{image}', b'1', 0)".format(
                    sku=sku,
                    name=sql_escape(name),
                    description=sql_escape(description),
                    category=sql_escape(category),
                    price=price,
                    stock=stock,
                    image=image_path,
                )
            )

    return rows, index


def write_migration(path, rows):
    sql = (
        "-- Generated by tools/generate_catalog.py. Do not edit by hand.\n"
        "INSERT INTO products (sku, name, description, category, price, stock_quantity, image_path, active, version)\n"
        "VALUES\n"
        + ",\n".join(rows)
        + ";\n"
    )
    path.write_text(sql)


def main():
    IMAGE_DIR.mkdir(parents=True, exist_ok=True)

    rows, index = build_rows(CATALOG, 0)
    assert index == 50, f"expected 50 products, generated {index}"
    write_migration(MIGRATION, rows)

    produce_rows, produce_index = build_rows(PRODUCE_CATALOG, index)
    assert produce_index == 70, f"expected 70 products, generated {produce_index}"
    write_migration(PRODUCE_MIGRATION, produce_rows)

    print(
        f"Wrote {MIGRATION.relative_to(ROOT)}, {PRODUCE_MIGRATION.relative_to(ROOT)} "
        f"and {produce_index} images to {IMAGE_DIR.relative_to(ROOT)}"
    )


if __name__ == "__main__":
    main()
