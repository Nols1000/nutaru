---
title: "Plate detection (vision)"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Plate-photo logging path via Gemma 4 vision. Quick Add Photo mode (placeholder in #8) now functional: camera capture or gallery pick. Image sent to Gemma 4 with vision prompt requesting structured identification: list of food items with portions. Returns multi-item proposal. Batch confirm UI: single bottom sheet lists all identified items, user reviews and edits each (quantity, unit, meal type), confirms all in one tap (single transaction). Assistant emits a text description string alongside the structured proposal (e.g., "chicken breast ~150g, rice ~1 cup, broccoli") so screen readers can announce what was identified. Combined text+photo input supported: user types caption ("chicken bowl from Chipotle") + attaches photo for better ID than photo alone.

## Acceptance criteria

- [ ] Photo capture from Quick Add routes through Gemma 4 vision input pipeline
- [ ] Multi-item proposal (3+ items on typical meals) returned in structured form
- [ ] Batch confirm UI shows all items, each editable (quantity, unit, meal type)
- [ ] Single "confirm all" tap commits all entries in one DB transaction
- [ ] Alt-text description string emitted alongside proposal, screen reader announces on focus
- [ ] Text+photo input: caption + photo yields better ID precision than photo alone (qualitative eval)
- [ ] Plate-detect end-to-end latency under perf budget on iPhone 17 Pro (< 5s) and Pixel 5 (< 8s)
- [ ] NPE held-out 10-image set: ≥ 65% top-3 item precision before merge

## Blocked by

- `docs/issue-10-assistant-foundation-nl-text-logging.md`
