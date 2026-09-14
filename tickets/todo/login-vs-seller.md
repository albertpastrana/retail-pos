# Login no longer matches who is selling

Captured: 2026-09-14

Related: `tickets/todo/operator-docs.md`, `tickets/todo/live-shared-tickets.md`, `tickets/done/till-sales-screen-buttons.md`

## Goal

Who sold the ticket and who may use stock, close cash, or admin are two different questions. The till should treat them as such. Staff should not pretend to “be” someone at login and then sell as anyone else.

## Context

Login still picks a `PEOPLE` row (`JRootApp`). That user owns the session: side menu, `hasPermission` (pay, discounts, refund, ticket edit, close cash, people, stock). Passwords are empty on the seed users, so login is often one tap.

On sales, the seller buttons (`JTicketsBagShared.switchToSeller`) park the current ticket and open or create that person’s ticket. The receipt’s `PERSON` / `TicketInfo.user` is the seller. After the first switch, the logged-in user is not who the sale is attributed to. Permissions do not follow the button.

So login identity is theatre: you enter as one person, then you can be any other. Reports and receipts follow the button. Dangerous menus follow whoever tapped login.

This matters. A guest login can sell as the administrator’s name, or an administrator login can leave the full menu open while someone else is “selling”. Close cash, refunds, and people admin are not tied to the face on the ticket.

### Options

**A.** Till opens into sales with a generic till role (no person login). Seller buttons are who the sale belongs to. Stock, people, close cash, refunds, config: ask for a person who has that permission (password if they have one).

**B.** Same split, but one “open till” step at start of day (shop/till account), then A.

**C.** Seller switch asks for that person’s password (or card). Session user and permissions follow the button. The login screen is the same check; keep one of the two, not both.

**D.** Remove seller switching as identity. Logout to change person. That undoes per-person tickets.

**Recommendation:** A (or B if you want an explicit open-till). Shop floor is already “tap the person, get their ticket”. Do not PIN every seller change. Gate the privileged screens instead. C is the right model only if sellers must not use each other’s tickets without a code.

Do not: leave both login-as-person and free seller switch. Do not invent a second people table. `PEOPLE` stays; role is for privileged actions, seller is for the ticket.

Not decided: exact till-role permissions; whether refunds need a supervisor; satellite till (no payment) vs central.

## Done when

- Starting the till does not require choosing a person whose name will be ignored on the next seller tap.
- A sale is attributed to the seller button in use, not to whoever opened the session.
- Close cash, people, stock, and other privileged menus cannot be used just because someone logged in as Administrator and then switched seller.
- Operator docs describe open-till vs choose-seller vs supervisor, not “login as yourself then pick another face”.
