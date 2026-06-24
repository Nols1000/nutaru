---
title: "Beta release infrastructure"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Beta release pipeline and store readiness. Fastlane `supply` (Play) and `deliver` (App Store) configured for repeatable submissions. App Store and Play Store listings: English copy (title, subtitle, description, keywords, promo text), screenshots (6–8 per device size, English-only), age rating 4+ (Apple) / Everyone (Google), App Store privacy labels all marked "No", Play data safety form all zeros. Privacy / data-processing notice published at `nutaru.app/privacy` (GitHub Pages), explaining all-on-device processing, no data controller, no data processor, GDPR/CCPA posture. TestFlight external testing configured with first beta build uploaded. Play Internal Testing configured with first build uploaded. Beta cohort recruitment plan executed: posts drafted for r/QuantifiedSelf, r/privacy, HN, Mastodon fediverse; Discord feedback channel ready. 2–4 week feedback window, ≥ 10 active testers giving feedback required to ship.

## Acceptance criteria

- [ ] Fastlane produces signed release builds for both platforms (one command per store)
- [ ] App Store listing: title, subtitle, description, keywords, screenshots submitted and approved
- [ ] Play Store listing: equivalent fields submitted and approved
- [ ] App Store privacy labels: all categories marked "No"
- [ ] Play data safety form: all zeroes
- [ ] Privacy doc live at `nutaru.app/privacy`, references AGPL source, explains all-on-device processing
- [ ] Age rating confirmed 4+ (Apple) / Everyone (Google)
- [ ] TestFlight external testing configured, first build uploaded and distributed to beta cohort
- [ ] Play Internal Testing configured, first build uploaded and distributed
- [ ] Beta recruitment posts drafted and ready to publish
- [ ] Discord feedback channel created with structured intake (bug reports, feature requests, general)
- [ ] Crash-free session rate ≥ 99% over 2-week beta window
- [ ] No P0 crash present in > 1% of sessions

## Blocked by

- `docs/issue-15-accessibility-nfr-audit.md`
- `docs/issue-16-nae-npe-eval-harness.md`
