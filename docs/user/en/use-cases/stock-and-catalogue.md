# Stock and catalogue

**Situation:** Staff need to find products, create one from the catalogue, or receive stock.

## Find and create a product

1. From sales, search by name, reference/model, category, brand, or family.
2. If an EAN is in the shared catalogue, the till can open the create dialog with the product data.
3. Check the name, reference, size, colour, price, and category.
4. Save only when the data is correct.

One sellable product row represents one EAN. Sizes and colours are variants of the model, not attributes that require a choice after scanning.

## Unknown EAN

If the EAN exists neither in `PRODUCTS` nor in the fallback catalogue, the sales screen shows a notice below the keypad and does not open a create dialog. Keep the code and give it to the stock person.

The shared `PENDING_BARCODES` queue is not available yet. Do not assume that another till can see this notice.

## Receiving stock

Record received goods in **Stock → Stock diary**. Importing or creating a product does not create stock. An order is an intention to buy; a stock-diary entry is the movement that increases stock.

## Imports and prices

TSV files are prepared outside the application. The importer is an initial-loading tool, not a backup. Price rules can propose a selling price, but always review exceptional variants before saving.
