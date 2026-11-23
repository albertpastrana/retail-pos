# External voucher payment

Captured: 2026-11-23

Related: `tickets/done/shared-configurable-loyalty-settings.md`, `tickets/done/gift-vouchers.md`

## Goal

Accept externally redeemed vouchers as a separate, mixed-payment tender with a shared configurable display name and an accurate historical name on each sale.

## Context

The default display name is "External voucher"; the store can set it to a campaign name such as "iBons" in the database-backed payment configuration shared by tills. Accept any positive amount up to the remaining sale balance, with no change or fixed denominations. The cashier validates and redeems the external voucher separately; this POS does not integrate with the issuer or refund external vouchers. Keep this tender distinct from cash and internally issued gift vouchers. A later name change must not rename earlier sales, receipts or report rows. The payment method uses a stable internal key and snapshots its label per payment.

Operator feedback: the voucher numeric keypad should use the compact cash-payment arrangement, an entered amount above the remaining balance must show the reason why it cannot be applied, and the external-voucher tab should sit directly below the internally issued voucher tab with a distinct icon.

## Done when

- A cashier can enter an external voucher amount, apply part of a sale, undo it, and pay the balance by cash or card; zero, negative and excess amounts cannot be applied.
- The shared configuration displays a generic name by default and allows a new name to be saved once for all tills.
- Sales, reprints, recent sales, payment reports and cash closing show the voucher separately, retaining each sale's configured name after a change.
- External vouchers never count as physical cash, produce change, or appear as a refund option; existing gift voucher flows remain intact.
- English, Catalan and Spanish UI and receipt labels and focused migration, persistence and mixed-payment checks pass.
- The external-voucher entry matches the cash panel's keypad sizing, explains an excessive amount in all three languages, and appears directly below the voucher tab with Lucide `ticket-check`. Do not reuse the cash-like `menu-payments.png`.

## Progress

Implemented locally on `feature/external-voucher-payment`: shared database name, independent external tender, partial payments, historical payment labels, receipt templates, closing and report breakdown, role permissions and V61 migration. The generic default remains localized until a store explicitly saves a name. The payment screen prompts the cashier to redeem with the issuer; no issuer connection or external-voucher refund is included.

`./gradlew spotlessApply`, focused payment and layout tests in three locales, `./gradlew check` and `docker compose up -d --wait` / `./gradlew ciCheck` / `docker compose down -v` passed against Derby, MySQL and PostgreSQL test databases. The test containers were stopped. The live sales checkout and configuration screens have not been opened and manually checked for keyboard, focus, resize, labels and real receipt printing; keep this ticket open until that verification and the pull-request workflow are complete. Unrelated working-tree changes remain untouched.

Operator feedback addressed locally: the external-voucher keypad now sits to the right at its preferred size, as in cash payment; an excessive entry explains the maximum payable amount instead of only disabling confirmation, with an additional message for zero/invalid input. The tab follows the internal voucher tab and uses a distinct payments icon. A focused Swing test checks keypad height, partial/full/invalid states and localized feedback in English, Spanish and Catalan; `./gradlew spotlessApply`, the focused test, `./gradlew check`, and `git diff --check` pass. Hands-on visual and cashier-flow review remains outstanding.

Screenshot review found that the payments icon looks like cash and must be removed until the operator selects a replacement. The Catalan message rendered a literal `{0}` because its apostrophe was interpreted by MessageFormat. An invalid value also leaves an old partial amount in the disabled action. The operator needs a plain-language limit and a neutral disabled action, verified against the screenshot scenario (55 entered with 40 due) in all three languages. The payment dialog is also too wide: use a narrower amount field, wrap long instructions/feedback, and keep the partial-payment action concise. The existing cash and internal voucher icons are `cash.png` and `knotes.png` under `src-beans/com/openbravo/images/`; the rejected external-voucher icon was `menu-payments.png` in the same folder.

The screenshot fixes now show the amount still due directly, clear the stale partial-payment amount when invalid, and leave the external-voucher tab iconless pending the operator's choice. The entry uses an 8-column field, 22-column wrapped hints and a short partial action; the keypad retains cash's preferred-size placement. Automated Swing coverage uses 55 entered against 40 due in EN/ES/CA, checks that `{0}` is never rendered, that the disabled action has no stale amount, and that the panel's preferred width stays below 700 at the test font size. `./gradlew spotlessApply`, the focused test, `./gradlew check` and `git diff --check` pass; operator confirmation of the visual result is still pending.

The operator selected Lucide `ticket-check` for this payment tab. A new screenshot at the shop's display scale shows the dialog is still far too wide despite the 22-column hints: Swing computes the preferred width of the initial unwrapped text. Bound the preferred width of both hints from font metrics so the long strings actually wrap at pack time; check at the larger display scale, not only the default-font test size.

Added the upstream Lucide `ticket-check` SVG to the shared runtime icon set and wired it to the tab via `PosIcons`. The 22-column constructor still overrode the intended hint width; zero-column text areas with a content/font-derived preferred width now let instructions and errors wrap before `pack()`. Reduced the amount field to six columns. A regression check compares the voucher and cash panels at double the UI font size in English, Spanish and Catalan; `./gradlew spotlessApply`, the focused test, `./gradlew check`, and `git diff --check` pass. The actual till screenshot still needs operator review before calling the visual problem solved.

The next live screenshot shows a new regression: the six-column amount input is barely wide enough for a digit and the form leaves excess blank vertical space beneath the partial-payment action. Size the input for a realistic locale-formatted amount and keep help/action at the top of the form without expanding the dialog again. Treat this screenshot as the visual check for this iteration.

Changed the amount field from an undersized character count to a preferred width based on the label font and a representative currency amount. Shortened the reserved instruction/error rows and top-aligned the form; the shared payment dialog and keypad remain untouched. Focused Swing checks in English, Spanish and Catalan cover the field's currency-width minimum and the compact comparison with the cash panel at double font size. `./gradlew spotlessApply`, the focused test, `./gradlew check`, and `git diff --check` pass. A new cashier screenshot is still needed to confirm the displayed field and spacing at the actual till scale.

The operator requested a pull request, green CI and CodeRabbit review, then a rebase merge into main. The final operator screenshot predates the amount-width correction; automated UI checks cover the revised form at normal and doubled font sizes, while a final hands-on screenshot remains unverified.

PR #168 passed the initial GitHub checks. CodeRabbit identified a stale shared-name overwrite on unrelated settings saves, a separate loyalty-connection failure path, malformed/negative input handling, two order-dependent or overly broad assertions, and noncanonical receipt templates skipped by V61. Fix the valid findings before merging. V61 is already committed: preserve it and use V62 for the receipt-template upgrade repair, with an upgrade test and a checksum entry.

Review fixes: only edited external-voucher names are written to the shared settings row, using the settings connection independently of loyalty; untouched saves omit the name. Full-input validation rejects malformed numbers and a pasted minus sign is retained as an invalid value. Tests now assert voucher-specific errors and locate persisted vouchers by type. V62 repairs existing receipt templates with equivalent single-quoted or spaced cash conditions and warns for templates that cannot be patched; V61 remains unchanged. Added focused configuration and migration-upgrade tests. Re-run all checks and CodeRabbit after pushing this follow-up commit.
