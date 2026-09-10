#!/usr/bin/env python3
"""Add invoice costs to the barcode-keyed scan catalogue.

The supplier invoices identify a model, while the till looks products up by
EAN. This script expands a model cost to every matching catalogue variant and
only fills EANs which do not already have a better manufacturer/catalogue cost.
"""

from __future__ import annotations

import argparse
import csv
import re
import unicodedata
from collections import defaultdict
from datetime import datetime
from decimal import Decimal, ROUND_CEILING
from pathlib import Path

EQUIVALENCE_COST_FACTOR = Decimal("1.262")
TAX_FACTOR = Decimal("1.21")
DEFAULT_MARKUP_FACTOR = Decimal("1.475")
DESCRIPTION = "Descripció"
SUPPLIER = "Proveïdor"
GENERIC_WORDS = {
    "ALTA",
    "BAJA",
    "BIKINI",
    "BODY",
    "BRAGA",
    "CALCETIN",
    "CAMISETA",
    "COPA",
    "FAJA",
    "MUJER",
    "MODELO",
    "PACK",
    "PANTALON",
    "REDUCTOR",
    "SELENE",
    "SENORA",
    "SLIP",
    "SUJETADOR",
}


def normalized(value: str) -> str:
    return "".join(
        char for char in unicodedata.normalize("NFKD", value.upper()) if char.isalnum()
    )


def words(value: str) -> set[str]:
    return {
        normalized(word)
        for word in re.findall(r"[A-Za-zÀ-ÿ]+", value)
        if len(normalized(word)) >= 4 and normalized(word) not in GENERIC_WORDS
    }


def same_product_kind(description: str, product_name: str) -> bool:
    description = normalized(description)
    product_name = normalized(product_name)
    for kind in ("BODY", "BRAGA", "SUJETADOR"):
        if kind in description:
            return kind in product_name
    return True


def valid_ean(value: str) -> bool:
    if not re.fullmatch(r"\d{8}|\d{12,14}", value):
        return False
    digits = [int(char) for char in value]
    return (sum(digits[-2::-2]) * 3 + sum(digits[-3::-2]) + digits[-1]) % 10 == 0


def decimal_value(value: str) -> Decimal:
    return Decimal(value.strip().replace(",", "."))


def parse_date(value: str) -> datetime:
    return datetime.strptime(value.strip(), "%d/%m/%Y")


def charm_round(value: Decimal) -> Decimal:
    if value >= Decimal("10"):
        euros = int(value)
        candidate = Decimal(euros) + Decimal("0.95")
        return candidate if candidate >= value else candidate + Decimal("1")
    euros = int(value)
    for ending in ("0.25", "0.50", "0.75", "0.95"):
        candidate = Decimal(euros) + Decimal(ending)
        if candidate >= value:
            return candidate
    return Decimal(euros + 1) + Decimal("0.25")


def product_model(row: list[str]) -> str:
    reference, brand = row[1], row[7]
    if brand in {"Abanderado", "Gisela", "Playtex"}:
        return reference.split()[0]
    if brand in {"Avetset", "Massana"}:
        return reference.split("-")[0]
    if brand == "Selene":
        return reference
    match = re.match(r"\d{4,8}", reference)
    return match.group(0) if match else reference


def selene_matches(
    invoice: dict[str, str], products: list[list[str]]
) -> list[list[str]]:
    code_parts = invoice["Codi"].split()
    if invoice["Codi"].startswith("2850 ") and len(code_parts) > 1:
        supplier_model = code_parts[1].zfill(5)
        expected = "06" + supplier_model[-3:]
        direct = [
            row
            for row in products
            if row[1] == expected and same_product_kind(invoice[DESCRIPTION], row[3])
        ]
        if direct:
            return direct

    description_words = words(invoice[DESCRIPTION])
    candidates: list[tuple[int, list[str]]] = []
    for row in products:
        if not same_product_kind(invoice[DESCRIPTION], row[3]):
            continue
        common = description_words & words(row[3])
        if common:
            candidates.append((max(map(len, common)), row))
    if not candidates:
        return []
    strongest = max(score for score, _ in candidates)
    if strongest < 5:
        return []
    return [row for score, row in candidates if score == strongest]


def find_products(
    invoice: dict[str, str],
    products_by_brand: dict[str, list[list[str]]],
) -> list[list[str]]:
    code = invoice["Codi"].strip()
    description = normalized(invoice[DESCRIPTION])

    brand = ""
    model = ""
    if "AVET" in description:
        brand, model = "Avetset", code
    elif "GISELA" in description:
        brand, model = "Gisela", code.replace("-", "/")
    elif "YSABELMORA" in description:
        brand, model = "Ysabel Mora", code
    elif "SELENE" in description:
        brand = "Selene"
    elif code.startswith("1090 "):
        brand, model = "Avetset", code.split()[1].lstrip("0")
    elif code.startswith("2820 "):
        brand, model = "Gisela", code.split()[1]
    elif code.startswith("2850 "):
        brand = "Selene"
    elif invoice[SUPPLIER].startswith("JORDI RUIZ") and code.startswith("26"):
        brand, model = "Avetset", code[2:].lstrip("0")
    elif invoice[SUPPLIER].startswith("JORDI RUIZ") and code.startswith("61"):
        brand = "Selene"
    else:
        return []

    products = products_by_brand.get(brand, [])
    if brand == "Selene":
        return selene_matches(invoice, products)
    wanted = normalized(model)
    return [row for row in products if normalized(product_model(row)) == wanted]


def infer_brand(invoice: dict[str, str]) -> str:
    description = normalized(invoice[DESCRIPTION])
    for source_name, catalog_name in (
        ("ABANDERADO", "Abanderado"),
        ("AVET", "Avetset"),
        ("GISELA", "Gisela"),
        ("MASSANA", "Massana"),
        ("PLAYTEX", "Playtex"),
        ("SELENE", "Selene"),
        ("YSABELMORA", "Ysabel Mora"),
    ):
        if source_name in description:
            return catalog_name
    code = invoice["Codi"]
    if code.startswith("1090 "):
        return "Avetset"
    if code.startswith("2820 "):
        return "Gisela"
    if code.startswith("2850 "):
        return "Selene"
    return ""


def load_products(path: Path) -> list[list[str]]:
    with path.open(encoding="utf-8", newline="") as file:
        rows = list(csv.reader(file, delimiter="\t"))
    malformed = [index for index, row in enumerate(rows, 1) if len(row) < 8]
    if malformed:
        raise ValueError(f"Malformed product rows: {malformed[:10]}")
    invalid = [
        (index, row[2]) for index, row in enumerate(rows, 1) if not valid_ean(row[2])
    ]
    if invalid:
        raise ValueError(f"Invalid product EANs: {invalid[:10]}")
    return rows


def load_prices(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open(encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file, delimiter="\t")
        return list(reader.fieldnames or []), list(reader)


def build_additions(
    products: list[list[str]],
    existing_prices: list[dict[str, str]],
    invoices_path: Path,
) -> tuple[list[dict[str, str]], list[dict[str, str]]]:
    products_by_brand: dict[str, list[list[str]]] = defaultdict(list)
    for product in products:
        products_by_brand[product[7]].append(product)

    with invoices_path.open(encoding="utf-8", newline="") as file:
        invoices = list(csv.DictReader(file))
    invoices.sort(key=lambda row: parse_date(row["Data"]), reverse=True)

    priced_eans = {row["barcode"] for row in existing_prices}
    additions: list[dict[str, str]] = []
    unmatched: list[dict[str, str]] = []
    for invoice in invoices:
        matched = find_products(invoice, products_by_brand)
        if not matched:
            unmatched.append(invoice)
            continue

        base_cost = decimal_value(invoice["Preu Cost Unitari (EUR)"])
        discount = decimal_value(invoice["Dte %"] or "0")
        factory_cost = base_cost * (Decimal("1") - discount / Decimal("100"))
        factory_cost = factory_cost.quantize(Decimal("0.000001"))
        economic_cost = factory_cost * EQUIVALENCE_COST_FACTOR
        gross_sell = charm_round(economic_cost * DEFAULT_MARKUP_FACTOR)
        net_sell = (gross_sell / TAX_FACTOR).quantize(Decimal("0.000001"))
        source = (
            f"factura-{invoice['Núm. Factura']}-"
            f"{parse_date(invoice['Data']).strftime('%Y%m%d')}"
        )
        for product in matched:
            barcode = product[2]
            if barcode in priced_eans:
                continue
            additions.append(
                {
                    "barcode": barcode,
                    "reference": product[1],
                    "price_buy": f"{factory_cost:.6f}",
                    "price_sell": f"{net_sell:.6f}",
                    "brand": product[7],
                    "source": source,
                }
            )
            priced_eans.add(barcode)
    return additions, unmatched


def write_reference_prices(
    invoices_path: Path,
    output_path: Path,
    products: list[list[str]],
) -> None:
    products_by_brand: dict[str, list[list[str]]] = defaultdict(list)
    for product in products:
        products_by_brand[product[7]].append(product)
    with invoices_path.open(encoding="utf-8", newline="") as file:
        invoices = list(csv.DictReader(file))

    fields = [
        "supplier_reference",
        "description",
        "brand",
        "price_before_tax",
        "discount_percent",
        "price_buy",
        "supplier",
        "invoice",
        "date",
    ]
    rows = []
    for invoice in invoices:
        base_cost = decimal_value(invoice["Preu Cost Unitari (EUR)"])
        discount = decimal_value(invoice["Dte %"] or "0")
        factory_cost = base_cost * (Decimal("1") - discount / Decimal("100"))
        factory_cost = factory_cost.quantize(Decimal("0.000001"))
        matched = find_products(invoice, products_by_brand)
        rows.append(
            {
                "supplier_reference": invoice["Codi"],
                "description": invoice[DESCRIPTION],
                "brand": matched[0][7] if matched else infer_brand(invoice),
                "price_before_tax": f"{base_cost:.2f}",
                "discount_percent": f"{discount.normalize()}",
                "price_buy": f"{factory_cost:.6f}",
                "supplier": invoice[SUPPLIER],
                "invoice": invoice["Núm. Factura"],
                "date": parse_date(invoice["Data"]).strftime("%Y-%m-%d"),
            }
        )
    rows.sort(
        key=lambda row: (row["supplier_reference"], row["date"], row["description"])
    )
    with output_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fields, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)
    print(f"Wrote {len(rows)} invoice lines to {output_path}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="Update import-prices.tsv")
    parser.add_argument(
        "--products", type=Path, default=Path("data/import-products.tsv")
    )
    parser.add_argument("--prices", type=Path, default=Path("data/import-prices.tsv"))
    parser.add_argument(
        "--invoices",
        type=Path,
        default=Path("external-data/preus-cost-factures.csv"),
    )
    parser.add_argument(
        "--reference-prices",
        type=Path,
        default=Path("data/import-reference-prices.tsv"),
    )
    args = parser.parse_args()

    products = load_products(args.products)
    fields, prices = load_prices(args.prices)
    expected_fields = [
        "barcode",
        "reference",
        "price_buy",
        "price_sell",
        "brand",
        "source",
    ]
    if fields != expected_fields:
        raise ValueError(f"Unexpected price columns: {fields}")

    base_prices = [row for row in prices if not row["source"].startswith("factura-")]
    additions, unmatched = build_additions(products, base_prices, args.invoices)
    covered_models = len({(row["brand"], row["reference"]) for row in additions})
    print(
        f"products={len(products)} base_prices={len(base_prices)} "
        f"additions={len(additions)} covered_references={covered_models} "
        f"unmatched_invoice_lines={len(unmatched)}"
    )
    if not args.write:
        print("Dry run; pass --write to update the lookup table.")
        return

    all_prices = base_prices + additions
    all_prices.sort(key=lambda row: (row["brand"], row["reference"], row["barcode"]))
    with args.prices.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=expected_fields, delimiter="\t")
        writer.writeheader()
        writer.writerows(all_prices)
    print(f"Wrote {len(all_prices)} rows to {args.prices}")
    write_reference_prices(args.invoices, args.reference_prices, products)


if __name__ == "__main__":
    main()
