# Gift vouchers

Captured: 2026-09-12

Related: `tickets/done/loyalty-stamps-on-ticket.md`

## Goal

Sell and redeem gift vouchers as products (`ISVOUCHER`). That is not the loyalty −5 € line.

## Done when

Voucher products exist in the catalogue and till lines know they are vouchers (so they do not earn stamps).

## Shipped

`PRODUCTS.ISVOUCHER` (migration `V11`), product row and ticket line flags, `LoyaltyStamps` excludes gift voucher lines from eligible spend.
