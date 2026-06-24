---
title: "NAE + NPE eval harness"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Standalone eval module under `eval/` directory at repo root. **Nutaru Agent Eval (NAE)**: 50 hand-authored golden queries across categories — `log_food`, `log_weight`, `query_macros`, `query_history`, `set_target`, `suggest_delete`, natural-language parse, plate-caption. Three grading facets per query: tool selection (binary — correct tool called), tool args valid (binary — passes schema), response sensible (manual 1–5 rating, ≥ 3 passes). **Nutaru Plate Eval (NPE)**: 50 manually-labeled plate photos across cuisines (Western, Asian, Latin American, Middle Eastern) and complexity tiers (simple 1–2 items, typical 3–5, complex 6+). 10-image held-out "live" set, never used during dev iteration. Grading: top-3 item precision (predicted vs labeled) is primary metric; portion accuracy within ±30% informational at V1. CI integration: smoke subset (10 NAE queries) runs per PR (blocking), full NAE + NPE runs nightly and pre-release.

## Acceptance criteria

- [ ] NAE 50-query suite authored with expected outputs, versioned alongside model versions
- [ ] NPE 50-image set labeled with ground-truth items (and portions where known); 10 held out
- [ ] CI: smoke 10 NAE queries runs per PR, blocks merge on failure
- [ ] CI: full NAE + NPE runs nightly, results tracked over time (regression detection)
- [ ] Pre-release gate: full NAE ≥ 85% pass rate, NPE ≥ 65% top-3 precision on held-out 10
- [ ] NAE/NPE versioned alongside model versions for cross-version regression tracking
- [ ] Eval module has a runnable demo / self-check proving the harness executes end-to-end against a real model

## Blocked by

- `docs/issue-10-assistant-foundation-nl-text-logging.md`
- `docs/issue-11-plate-detection.md`
- `docs/issue-12-assistant-chat-remaining-crud-tools.md`
