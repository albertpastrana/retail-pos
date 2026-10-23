# Diagnose ticket close failures

Captured: 2026-10-23

## Goal

Make failed ticket closing attempts diagnosable from structured application logs.

## Context

The sales screen currently logs a generic `ticket_close_failed` event when the
close flow returns false. The detailed cause is not always visible in the same
event, especially when payment is rejected or persistence returns false.

Add safe diagnostic fields and explicit failure reasons without logging payment
credentials, card data, or customer data.

## Done when

- Failed ticket close logs include ticket context, duration, and a failure reason.
- Payment rejection and persistence failure paths are distinguishable.
- Existing operator behaviour is unchanged.
- Relevant Java formatting and tests pass.
