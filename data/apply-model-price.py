#!/usr/bin/env python3
"""Set cost and ticket price on every variant of a model, then insert them.

Example:
  python3 data/apply-model-price.py Avet 3267 --cost 3.66 --price 5.95
  python3 data/apply-model-price.py Avet 3267 --cost 3.66 --price 5.95 --apply --insert
"""

from __future__ import annotations

import argparse
import csv
import subprocess
import sys
import tempfile
import unicodedata
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

TAX_FACTOR = Decimal("1.21")
PRICE_FIELDS = ["barcode", "reference", "price_buy", "price_sell", "brand", "source"]
YSABEL_MORA = "Ysabel Mora"
BRAND_ALIASES = {
    "AVET": "Avetset",
    "AVETSET": "Avetset",
    "YM": YSABEL_MORA,
    "YSABEL": YSABEL_MORA,
    "YSABELMORA": YSABEL_MORA,
    "ISABELMORA": YSABEL_MORA,
    "ISABEL": YSABEL_MORA,
}


def normalized(value: str) -> str:
    return "".join(
        char for char in unicodedata.normalize("NFKD", value.upper()) if char.isalnum()
    )


def money(value: str) -> Decimal:
    return Decimal(value.strip().replace(",", ".")).quantize(Decimal("0.01"))


def stored(value: Decimal) -> str:
    return f"{value.quantize(Decimal('0.000001')):.6f}"


def catalog_brand(value: str) -> str:
    key = normalized(value)
    if key in BRAND_ALIASES:
        return BRAND_ALIASES[key]
    return value.strip()


def product_model(row: list[str]) -> str:
    reference, brand = row[1], row[7]
    if brand in {"Abanderado", "Gisela", "Playtex"}:
        return reference.split()[0]
    if brand in {
        "Avetset",
        "Massana",
        "Petrus",
        "Dusen",
        "Señoretta",
        "Egatex",
        "Soy",
        "Muslher",
        "Focenza",
        "Punto Blanco",
        "Ruipérez",
        "Intimalia",
    }:
        return reference.split("-")[0]
    if brand == "Selene":
        return reference.split()[0]
    return reference


def parse_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, raw = line.split("=", 1)
        values[key.strip()] = raw.replace("\\:", ":").replace("\\=", "=")
    return values


def load_products(path: Path) -> list[list[str]]:
    with path.open(encoding="utf-8", newline="") as file:
        return list(csv.reader(file, delimiter="\t"))


def load_prices(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open(encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, delimiter="\t")
        return list(reader.fieldnames or []), list(reader)


def find_variants(products: list[list[str]], brand: str, model: str) -> list[list[str]]:
    wanted_brand = catalog_brand(brand)
    wanted_model = normalized(model)
    matches = [
        row
        for row in products
        if len(row) >= 8
        and row[7] == wanted_brand
        and normalized(product_model(row)) == wanted_model
    ]
    if matches:
        return matches
    prefix = [
        row
        for row in products
        if len(row) >= 8
        and row[7] == wanted_brand
        and normalized(row[1]).startswith(wanted_model)
    ]
    models = sorted({product_model(row) for row in prefix})
    if len(models) == 1:
        return prefix
    if models:
        raise SystemExit(
            f"Ambiguous {wanted_brand} {model}; matches {', '.join(models)}"
        )
    return []


def write_prices(path: Path, rows: list[dict[str, str]]) -> None:
    rows = sorted(
        rows, key=lambda row: (row["brand"], row["reference"], row["barcode"])
    )
    with path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=PRICE_FIELDS, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)


def insert_rows(properties: Path, categories: Path, batch: Path, repo: Path) -> str:
    config = parse_properties(properties)
    url = config.get("db.URL")
    if not url:
        raise SystemExit(f"No db.URL in {properties}")
    compile_helpers(repo)
    libs = repo / "build/runtime-libs"
    classpath = ":".join(
        [str(repo / "build/classes/java/dataHelpers")]
        + [str(path) for path in sorted(libs.glob("*.jar"))]
    )
    command = [
        "java",
        "-cp",
        classpath,
        "InsertCatalogRows",
        url,
        config.get("db.user", ""),
        config.get("db.password", ""),
        str(categories),
        str(batch),
    ]
    result = subprocess.run(
        command, cwd=repo, check=True, capture_output=True, text=True
    )
    if result.stderr:
        sys.stderr.write(result.stderr)
    return result.stdout.strip()


def compile_helpers(repo: Path) -> None:
    subprocess.run(
        ["./gradlew", "-q", "compileDataHelpersJava"],
        cwd=repo,
        check=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("brand")
    parser.add_argument("model")
    parser.add_argument("--cost", required=True, help="Buy price before tax")
    parser.add_argument("--price", required=True, help="Ticket price including VAT")
    parser.add_argument("--apply", action="store_true", help="Write import-prices.tsv")
    parser.add_argument(
        "--insert", action="store_true", help="Insert or update PRODUCTS"
    )
    parser.add_argument(
        "--products", type=Path, default=Path("data/import-products.tsv")
    )
    parser.add_argument("--prices", type=Path, default=Path("data/import-prices.tsv"))
    parser.add_argument(
        "--categories", type=Path, default=Path("data/import-categories.tsv")
    )
    parser.add_argument("--properties", type=Path, default=Path("dev.properties"))
    parser.add_argument("--source", default="manual")
    args = parser.parse_args()

    cost = money(args.cost)
    gross = money(args.price)
    sell = (gross / TAX_FACTOR).quantize(Decimal("0.000001"), rounding=ROUND_HALF_UP)
    products = load_products(args.products)
    variants = find_variants(products, args.brand, args.model)
    if not variants:
        raise SystemExit(f"No variants for {catalog_brand(args.brand)} {args.model}")

    print(
        f"{catalog_brand(args.brand)} {args.model}: {len(variants)} variants, "
        f"cost {cost} → ticket {gross} (stored sell {sell})"
    )
    for row in variants:
        print(f"  {row[1]}\t{row[2]}\t{row[3]}")

    if not args.apply and not args.insert:
        print("Dry run; pass --apply to save prices, --insert to create till products.")
        return

    fields, prices = load_prices(args.prices)
    if fields != PRICE_FIELDS:
        raise SystemExit(f"Unexpected price columns: {fields}")
    by_barcode = {row["barcode"]: row for row in prices}
    for row in variants:
        by_barcode[row[2]] = {
            "barcode": row[2],
            "reference": row[1],
            "price_buy": stored(cost),
            "price_sell": stored(sell),
            "brand": row[7],
            "source": args.source,
        }
    if args.apply:
        write_prices(args.prices, list(by_barcode.values()))
        print(f"Wrote {len(by_barcode)} price rows to {args.prices}")

    if args.insert:
        if not args.properties.is_file():
            raise SystemExit(f"Missing {args.properties}")
        with tempfile.NamedTemporaryFile(
            "w", encoding="utf-8", suffix=".tsv", delete=False
        ) as handle:
            for row in variants:
                handle.write(
                    "\t".join(
                        [
                            row[0],
                            row[1],
                            row[2],
                            row[3],
                            row[4],
                            stored(cost),
                            stored(sell),
                            row[7],
                        ]
                    )
                    + "\n"
                )
            batch = Path(handle.name)
        try:
            print(insert_rows(args.properties, args.categories, batch, Path.cwd()))
        finally:
            batch.unlink(missing_ok=True)


if __name__ == "__main__":
    main()
