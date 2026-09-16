#!/usr/bin/env python3
"""Walk a family grid (size × colour, canvas order) and scan missing EANs.

Example:
  python3 data/scan-family-barcodes.py 31460
"""

from __future__ import annotations

import argparse
import csv
import importlib.util
import shutil
import subprocess
from pathlib import Path
from urllib.parse import quote, urlparse, urlunparse

ROOT = Path(__file__).resolve().parent.parent
HELPERS = importlib.util.spec_from_file_location(
    "apply_model_price", Path(__file__).with_name("apply-model-price.py")
)
assert HELPERS and HELPERS.loader
amp = importlib.util.module_from_spec(HELPERS)
HELPERS.loader.exec_module(amp)

PSQL_CANDIDATES = [
    "psql",
    "/Applications/Postgres.app/Contents/Versions/latest/bin/psql",
    "/Applications/Postgres.app/Contents/Versions/18/bin/psql",
]

BASICS = ("ARENA", "BLANCO", "NEGRO")
LETTER = {"M", "G", "E", "EG"}
SIZE_ORDER = ["M", "G", "E", "EG", "44", "48", "52", "56", "6-8", "10-12", "14-16"]
COLOR_LABEL = {"ARENA": "arena", "BLANCO": "blanc", "NEGRO": "negre"}


def find_psql() -> str | None:
    for candidate in PSQL_CANDIDATES:
        path = shutil.which(candidate) if "/" not in candidate else candidate
        if path and Path(path).is_file():
            return path
    return None


def jdbc_to_postgres_url(jdbc: str, user: str, password: str) -> str:
    if not jdbc.startswith("jdbc:postgresql:"):
        raise SystemExit(f"Only PostgreSQL is supported here: {jdbc}")
    parsed = urlparse(jdbc[len("jdbc:") :])
    auth = quote(user, safe="")
    if password:
        auth += ":" + quote(password, safe="")
    host = parsed.hostname or "127.0.0.1"
    netloc = f"{auth}@{host}"
    if parsed.port:
        netloc += f":{parsed.port}"
    return urlunparse(("postgresql", netloc, parsed.path, "", "", ""))


def load_till_rows(properties: Path) -> list[tuple[str, str, str]]:
    config = amp.parse_properties(properties)
    url = config.get("db.URL")
    if not url:
        raise SystemExit(f"No db.URL in {properties}")
    psql = find_psql()
    if not psql:
        raise SystemExit("psql not found; install Postgres client or Postgres.app")
    pg_url = jdbc_to_postgres_url(
        url, config.get("db.user", ""), config.get("db.password", "")
    )
    result = subprocess.run(
        [
            psql,
            pg_url,
            "-t",
            "-A",
            "-F",
            "\t",
            "-c",
            "SELECT CODE, REFERENCE, NAME FROM PRODUCTS",
        ],
        check=True,
        capture_output=True,
        text=True,
    )
    rows: list[tuple[str, str, str]] = []
    for line in result.stdout.splitlines():
        if not line.strip():
            continue
        code, _, rest = line.partition("\t")
        reference, _, name = rest.partition("\t")
        rows.append((code, reference, name))
    return rows


def parse_color_size(reference: str, name: str) -> tuple[str, str]:
    size = reference.split("-")[-1] if "-" in reference else ""
    color = ""
    if " — " in name:
        rest = name.split(" — ", 1)[1].split(" [")[0]
        if " / " in rest:
            color, size = rest.split(" / ", 1)
    return color.strip().upper(), size.strip().upper()


def size_key(size: str) -> tuple[int, str]:
    return (SIZE_ORDER.index(size) if size in SIZE_ORDER else 80, size)


def color_key(color: str) -> tuple[int, str]:
    order = list(BASICS)
    return (order.index(color) if color in order else 10, color)


def show_color(color: str) -> str:
    return COLOR_LABEL.get(color, color.lower())


def models_for(products: list[list[str]], model: str) -> list[list[str]]:
    wanted = amp.normalized(model)
    return [
        row
        for row in products
        if len(row) >= 8 and amp.normalized(amp.product_model(row)) == wanted
    ]


def till_model(reference: str) -> str:
    return reference.split("-")[0] if "-" in reference else reference.split()[0]


def no_letter_e(family: str) -> bool:
    """Bikini (33…) and tanga (34…) families are not made in size E."""
    return family.startswith("33") or family.startswith("34")


def expected_grid(
    variants: list[list[str]], family: str = ""
) -> tuple[list[str], list[str]]:
    colors: set[str] = set()
    sizes: set[str] = set()
    for row in variants:
        color, size = parse_color_size(row[1], row[3])
        if color:
            colors.add(color)
        if size:
            sizes.add(size)
    if not variants:
        sizes_out = ["M", "G"] if no_letter_e(family) else ["M", "G", "E"]
        return sizes_out, list(BASICS)
    letter_sizes = sizes <= LETTER or bool(sizes & LETTER)
    if letter_sizes:
        expected_sizes = set(sizes) | {"M", "G", "E"}
        if "EG" not in sizes:
            expected_sizes.discard("EG")
        if no_letter_e(family):
            expected_sizes.discard("E")
    else:
        expected_sizes = set(sizes)
    if letter_sizes and colors & set(BASICS):
        expected_colors = set(colors) | set(BASICS)
    else:
        expected_colors = set(colors) or set(BASICS)
    return (
        sorted(expected_sizes, key=size_key),
        sorted(expected_colors, key=color_key),
    )


def overlay_prices(
    row: list[str], prices: dict[str, dict[str, str]]
) -> tuple[str, str]:
    price = prices.get(row[2])
    if price:
        return price["price_buy"], price["price_sell"]
    return row[5], row[6]


def write_tsv(path: Path, fieldnames: list[str], rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)


ABSENT_FIELDS = ["familia", "color", "talla"]


def append_absent(path: Path, rows: list[dict[str, str]]) -> None:
    existing: set[tuple[str, str, str]] = set()
    if path.exists():
        with path.open(encoding="utf-8", newline="") as file:
            for row in csv.DictReader(file, delimiter="\t"):
                existing.add((row["familia"], row["color"], row["talla"]))
    new_rows = [
        row
        for row in rows
        if (row["familia"], row["color"], row["talla"]) not in existing
    ]
    if not new_rows:
        return
    write_header = not path.exists()
    with path.open("a", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=ABSENT_FIELDS, delimiter="\t")
        if write_header:
            writer.writeheader()
        writer.writerows(new_rows)


def read_scan(prompt: str) -> str | None:
    try:
        line = input(prompt)
    except EOFError:
        return None
    return line.strip()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("family", nargs="?")
    parser.add_argument(
        "--products", type=Path, default=ROOT / "data/import-products.tsv"
    )
    parser.add_argument("--prices", type=Path, default=ROOT / "data/import-prices.tsv")
    parser.add_argument("--properties", type=Path, default=ROOT / "dev.properties")
    args = parser.parse_args()

    family = args.family or input("Família: ").strip()
    if not family:
        raise SystemExit("Cal la família")

    products = amp.load_products(args.products)
    variants = models_for(products, family)
    name = variants[0][3].split(" — ")[0] if variants else "?"
    sizes, colors = expected_grid(variants, family)
    print(
        f"{family}: {name} · graella {' '.join(sizes)} × {', '.join(show_color(c) for c in colors)}",
        flush=True,
    )
    print("Enter buit = no el tinc. Ctrl-C per parar.", flush=True)

    till_rows = load_till_rows(args.properties)
    till_by_slot: dict[tuple[str, str], tuple[str, str]] = {}
    till_by_code: dict[str, str] = {}
    for code, reference, pname in till_rows:
        till_by_code[code] = reference
        if till_model(reference) != family:
            continue
        color, size = parse_color_size(reference, pname)
        if color and size:
            till_by_slot[(color, size)] = (code, reference)

    catalog_by_slot: dict[tuple[str, str], list[str]] = {}
    catalog_by_code: dict[str, list[str]] = {}
    for row in variants:
        color, size = parse_color_size(row[1], row[3])
        catalog_by_code[row[2]] = row
        if color and size:
            catalog_by_slot[(color, size)] = row

    _, price_rows = amp.load_prices(args.prices)
    prices = {row["barcode"]: row for row in price_rows}

    scanned_new: list[dict[str, str]] = []
    catalog_missing: list[dict[str, str]] = []
    absent: list[dict[str, str]] = []
    skipped = 0
    already = 0

    for size in sizes:
        print(flush=True)
        for color in colors:
            print(f"Talla {size}", flush=True)
            print(f"Color {show_color(color)}", flush=True)
            slot = (color, size)
            if slot in till_by_slot:
                print(f"Talla {size} {show_color(color)} ja estava al TPV", flush=True)
                already += 1
                continue
            code = read_scan("Escaneja: ")
            if code is None:
                raise SystemExit("Fi")
            if not code or amp.normalized(code) == "NOTENIM":
                print("  (no el tinc)", flush=True)
                skipped += 1
                absent.append({"familia": family, "color": color, "talla": size})
                continue
            known = till_by_code.get(code)
            if known:
                print(f"  compte: aquest EAN ja és {known}", flush=True)
                skipped += 1
                continue
            catalog_row = catalog_by_slot.get(slot)
            if catalog_row is None and code in catalog_by_code:
                other = catalog_by_code[code]
                print(
                    f"  compte: aquest EAN és {other[1]}, no {family} {show_color(color)} {size}",
                    flush=True,
                )
                skipped += 1
                continue
            if catalog_row and catalog_row[2] == code:
                buy, sell = overlay_prices(catalog_row, prices)
                catalog_missing.append(
                    {
                        "id": catalog_row[0],
                        "reference": catalog_row[1],
                        "barcode": catalog_row[2],
                        "name": catalog_row[3],
                        "category": catalog_row[4],
                        "price_buy": buy,
                        "price_sell": sell,
                        "brand": catalog_row[7],
                    }
                )
                print(f"  {catalog_row[1]} (al catàleg)", flush=True)
            else:
                scanned_new.append(
                    {
                        "familia": family,
                        "barcode": code,
                        "color": color,
                        "talla": size,
                    }
                )
                print(f"  nou {code}", flush=True)

    print(flush=True)
    print(
        f"Ja al TPV {already}, nous {len(scanned_new)}, "
        f"catàleg {len(catalog_missing)}, no els tinc {skipped}"
    )
    if catalog_missing:
        path = ROOT / "data" / f"scan-{family}-del-cataleg.tsv"
        write_tsv(
            path,
            [
                "id",
                "reference",
                "barcode",
                "name",
                "category",
                "price_buy",
                "price_sell",
                "brand",
            ],
            catalog_missing,
        )
        print(f"Escrit {path}")
    if scanned_new:
        path = ROOT / "data" / f"scan-{family}-nous.tsv"
        write_tsv(path, ["familia", "barcode", "color", "talla"], scanned_new)
        print(f"Escrit {path}")
        print("Passa aquest fitxer a l'agent perquè prepari nom, categoria i insert.")
    if absent:
        append_absent(ROOT / "data/scan-absent.tsv", absent)
        print(f"NO TENIM: {len(absent)} (data/scan-absent.tsv)")
    if not scanned_new and not catalog_missing:
        print("Res de nou per inserir.")


if __name__ == "__main__":
    main()
