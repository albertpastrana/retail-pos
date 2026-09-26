# Stock and catalogue

**Situation:** Staff need to find products, create one from the catalogue, request replenishment, or receive stock.

## Find and create a product

1. From sales, search by name, reference/model, category, brand, or family.
2. If an EAN is in the shared catalogue, the till can open the create dialog with the product data. See the [fallback flow](product-not-found.md) for variants and opening stock.
3. Check the name, reference, size, colour, price, and category.
4. Save only when the data is correct.

One sellable product row represents one EAN. Sizes and colours are variants of the model, not attributes that require a choice after scanning.

## Unknown EAN

If the EAN exists neither in `PRODUCTS` nor in the fallback catalogue, the sales screen shows a notice below the keypad and does not open a create dialog. Keep the code and give it to the stock person.

The shared `PENDING_BARCODES` queue is not available yet. Do not assume that another till can see this notice.

## Replenishment and customer orders

1. Open **Replenishment and orders** and press **New replenishment request**.
2. Scan or enter the EAN. If the product is not found, use product search or enter a manual description.
3. If the request is for a customer, assign the customer and add a note if needed.
4. Save the request. No quantity is entered: each open entry means that the product or variant needs replenishing.
5. In the list, move the entry from **Pending** to **Ordered** when it has been requested from the supplier and to **Received** when it arrives. Reopen it if the order is cancelled or does not arrive.

An entry with a customer is an informational customer order or reservation. It does not create a sale, reserve stock, or create a receipt. Open entries for the same matched product are reused to avoid duplicates.

## Receiving stock

Record received goods in **Stock → Stock diary**. Marking a replenishment entry as **Received** does not update stock. An order is an intention to buy; a stock-diary entry is the movement that increases stock.

## Imports and prices

TSV files are prepared outside the application. The importer is an initial-loading tool, not a backup. Price rules can propose a selling price, but always review exceptional variants before saving.
