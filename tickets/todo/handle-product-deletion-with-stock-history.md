# Soft-delete products without losing history

Captured: 2026-09-29

Related: `tickets/in-progress/batch-stock-receiving.md`, `tickets/in-progress/simplify-product-search.md`

## Goal

Retire products without deleting their records, so they are unavailable for new sales or stock operations while their stock and sales history remains intact. Allow staff to find and reactivate them.

## Context

An attempt to delete product `muslher-1179450493911` on PostgreSQL failed on save at `DataLogicSales.getProductCatDelete()` with `STOCKSESSIONLINE_PRODUCT_FK`. `logs/pos-0.log` records `product_save_failed` on 2026-09-29 at 16:18:52 UTC. The products screen stages deletion, then saving tries `DELETE FROM PRODUCTS`; stock session lines retain a foreign key to products. Older stock diary and ticket lines also reference products. Do not delete or detach stock/transaction history simply to allow deletion.

Decision (2026-09-29): use soft-delete for **all** products, including unused ones. Add an `ISACTIVE` flag to `PRODUCTS`, defaulting to active for existing and newly created products; do not physically delete products or detach historical references. The current delete action becomes a clearly labelled deactivate action, with matching confirmation. Retain product IDs, references, and barcodes while inactive, so history remains unambiguous and unique codes cannot silently be reused.

Exclude inactive products from operational sales catalogues and product finders, stock searches and new receipt lines, and other new-product selection paths. Keep them visible to historical reports, tickets, stock diary and already-posted sessions. An open stock session referencing a newly inactive product must not post it as a new movement; show an actionable error while preserving its pending lines. Provide an explicit way to show inactive products in product administration and reactivate them. No remote database changes are part of this ticket.

## Done when

- Deactivating a product, whether used or unused, succeeds without a raw SQL error; its database row and existing stock-session, diary and sales references remain intact.
- Inactive products do not appear in new sales, catalogue selection or new stock-receipt lookups; historical tickets, reports, and posted stock sessions still display their product details.
- An open receipt containing a now-inactive product remains reviewable but cannot post that product without an actionable explanation.
- Product administration can show inactive products and reactivate one; after reactivation it is selectable again. Product codes and references stay reserved while inactive.
- Labels, confirmations and feedback are usable in English, Spanish and Catalan; relevant window sizes, keyboard/focus and finder flows are checked.
- A versioned migration covers Derby, MySQL and PostgreSQL, with existing products active by default and migration checksums updated. Integration coverage verifies deactivation, filtering, preserved history and reactivation.
