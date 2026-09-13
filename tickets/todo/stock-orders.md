# Stock orders (out of stock / to order)

Captured: 2026-09-10, 2026-09-11

Related: `tickets/todo/pending-barcodes.md`, `tickets/todo/postgres-lan.md`

## Goal

Staff can jot what is missing so they can place a supplier order: product or EAN, quantity, note, status. Not the stock diary.

## Context

Intended: `ORDERS` plus lines. From till or stock: add to the order. Stock panel: review, ordered, arrived.

App-owned tables on the shared database, not side files. Same queue on every PC.

This is not goods-in (`STOCKDIARY`) and not the unknown-barcode queue.

## Done when

A cashier or stock user can record a line, see it on every till, and move it through review → ordered → arrived.
