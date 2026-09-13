# Till sales-screen buttons

Captured: 2026-09-14

## Goal

Staff can tell till actions and sellers apart at arm's length: named buttons, icons the same size as avatars, and parked tickets labelled with person and time.

## Context

The top row mixed a leftover ticket-id box with 16 px icons and seller keys beside initials. Print and open-drawer lived as database images. Staff without a photo all used the same default picture.

Decisions: Phosphor Bold at 32 px display (64 px source), text under every standalone icon, seller keys under the avatar, ticket-id label removed, parked-ticket line is `Name · DD/MM - HH:mm`. Old 16 px assets stay for other screens.

## Done when

- Login and seller buttons show initials on a distinct colour when the user has no photo.
- Sales-row actions (customer, new, cancel, parked, print, open drawer) and the receipt-side column share icon size and a word under the icon.
- Parked-ticket list shows person plus date and time.
- Ticket-id label is gone from the sales screen.

## Shipped

`TillButtons` plus Phosphor assets under `images/till/`. Flyway V16/V17 replace Print and OpenDrawer images. Fonts scaled 10%. Avatar generation in `DataLogicSystem`.
