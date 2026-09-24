# Remove card reader and gateway integrations

Captured: 2026-09-24

## Goal

Keep card payments as a manual external-terminal tender while removing legacy card-reader and payment-gateway integrations.

## Context

The till should show the amount and ask the cashier to complete the payment on the external terminal, then record the payment locally. Card refunds keep the same manual flow. Remove reader, parser, gateway, provider configuration, and related translated UI; keep card payment and refund permissions, receipt output, mixed payments, and cash closing behaviour.

Existing worktree changes are unrelated and must not be included.

## Done when

- Card sale and refund flows use only the manual external-terminal panel.
- Card reader, parser, and payment-gateway implementations are absent from the build.
- Payment configuration no longer exposes or persists reader, gateway, or gateway test-mode settings.
- Card payments, refunds, mixed payments, receipts, and cash closing continue to work.
- English, Spanish, and Catalan UI no longer mention removed reader or gateway options.
- Relevant formatting and tests pass.

## Shipped

Removed the legacy card readers, parsers, payment gateways, provider configuration, and related properties. Card sales and refunds now use the external-terminal confirmation panel, with card receipts and permissions retained.

Key paths: `src-pos/com/openbravo/pos/payment/`, `src-pos/com/openbravo/pos/config/JPanelConfigPayment.java`, and `locales/pos_messages*.properties`.
