# Default refund voucher

Captured: 2026-10-11

## Goal

Select voucher as the default refund method when an operator starts a return.

## Context

Refunds currently open with cash selected even though the voucher refund flow
already creates a redeemable voucher. Cash, voucher, and card must remain
available as explicit alternatives.

## Done when

- Starting a refund selects the voucher method by default.
- The operator can still choose cash or card before confirming the refund.
- The generated voucher remains redeemable and its code is printed on the
  refund ticket.

## Shipped

Refunds now open with the voucher method selected first in
`JPaymentSelectRefund`; cash and card remain available. Verified with
`./gradlew spotlessApply` and `./gradlew test`.
