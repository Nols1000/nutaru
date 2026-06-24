---
title: "Custom food creation"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

User-created food items. Creation form collects: name, brand (optional), barcode (optional), nutrition per-100g (kcal + P/C/F + any tracked micros), serving definitions (original unit + gram equivalent for each common serving). Saves to `products` table with `source_id` referencing a user-owned source row. Custom foods searchable alongside pack products with rank parity. Pack items are immutable; if user wants to edit a pack item's nutrition or serving (e.g., incorrect OFF data), clone-on-edit creates a new user-owned `products` row with `derived_from` reference to the original. Custom foods are editable and deletable by the user.

## Acceptance criteria

- [ ] Custom food creation form with full validation (required fields, numeric ranges, serving sanity)
- [ ] Saved item appears in search results immediately, source indicator optional in UI
- [ ] Edit pack item: clones to user-owned row with `derived_from` set to original product id, original preserved
- [ ] Edit custom food: updates in place; existing `log_entries` referencing it reflect new values in rollups
- [ ] Delete custom food: confirm dialog, hard delete, warns if existing log entries reference it
- [ ] `commonTest`: clone-on-edit creates correct `derived_from` reference, original row unchanged
- [ ] `commonTest`: search results include user-owned items with FTS5 rank parity vs pack items
- [ ] `commonTest`: rollup math reflects edited custom food values for past log entries

## Blocked by

- `docs/issue-08-food-search-barcode-logging.md`
