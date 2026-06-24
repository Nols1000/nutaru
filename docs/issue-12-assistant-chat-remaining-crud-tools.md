---
title: "Assistant chat + remaining CRUD tools"
type: issue
status: ready-for-agent
created: 2026-06-24
parent: prd-nutaru-v1
---

## Parent

`docs/prd-nutaru-v1.md`

## What to build

Full conversational assistant surface. Chat thread UI (Bevel-style): message history with user and assistant turns, tool-call results rendered inline (e.g., "Logged 2 eggs for breakfast ✓"). Persistence via `agent_conversations` and `agent_messages` tables, multiple conversations supported with a conversations browser. Read tools wired end-to-end: `query_logs(dateRange, filters)`, `query_targets(date)`, `query_weight_history(dateRange)`, `aggregate_macros(dateRange, groupBy)`. Remaining write tools wired (all via suggest-confirm, never auto-commit): `log_weight`, `set_target`, `edit_entry`, `delete_entry`. Destructive proposals (edit/delete) flow through batch-confirm UX. Agent insight tile on dashboard (placeholder in #9) generates a weekly insight, refreshed nightly or on-demand via background task.

## Acceptance criteria

- [ ] Chat thread UI with user/assistant message bubbles, scrolls smoothly, supports multi-line input
- [ ] Conversations persist across launches, listed in a conversations browser
- [ ] Read tools return correct data: "how much protein this week?" → accurate aggregate
- [ ] `log_weight` tool: user types "I weigh 78kg today" → proposal → confirm → `weight_entries` write
- [ ] `set_target` tool: user asks to lower kcal target → proposal → confirm → `targets` write
- [ ] `edit_entry` and `delete_entry` tools: user asks "delete my second breakfast entry" → proposal → confirm → row updated/removed
- [ ] Destructive proposals render in batch-confirm UI, require explicit tap
- [ ] Agent insight tile generates weekly insight, displays on dashboard, refreshable on demand
- [ ] NAE full 50-query suite: ≥ 85% pass rate overall

## Blocked by

- `docs/issue-10-assistant-foundation-nl-text-logging.md`
