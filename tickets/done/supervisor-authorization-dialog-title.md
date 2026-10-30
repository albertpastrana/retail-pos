# Localize the supervisor authorization dialog title

Captured: 2026-10-10

## Goal

When a cashier attempts a privileged operation, the supervisor selector has a meaningful title in English, Spanish, and Catalan.

## Context

`SupervisorAuthorization.authorize` currently uses `title.editor` for the supervisor selector, so the Catalan dialog reads “Missatge de l'editor”. This shared selector is used for refunds, discounts, cash movements and closing, drawer opening, manual prices, closed-ticket editing, and debt payment. Keep authorization behaviour unchanged.

## Done when

- The selector title identifies supervisor authorization in English, Spanish, and Catalan.
- Existing privileged-operation call sites retain their authorization and cancellation behaviour.
- Targeted verification passes; manual checks are recorded accurately.

## Verification

The shared supervisor selector now uses `title.supervisorauthorization` with English, Spanish, and Catalan values. Its existing operation messages and authorization/cancellation logic are unchanged. `./gradlew spotlessApply integrationTest --tests com.openbravo.pos.forms.SupervisorAuthorizationTest` and `git diff --check` pass. Manual checks after privileged actions, including focus and cancellation, passed in all three languages at supported window sizes.

## Shipped

Localized the shared supervisor authorization selector title without changing authorization or cancellation behaviour. Automated and multilingual manual verification passed.
