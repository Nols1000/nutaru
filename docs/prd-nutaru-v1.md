---
title: "nutaru V1 — Local-first nutrition tracking with an intelligent local assistant"
type: prd
status: ready-for-agent
created: 2026-06-24
---

# nutaru V1

> *Local-first nutrition tracking with an intelligent local assistant. No account. No cloud. No subscription. On your device. Yours.*

## Problem Statement

Every serious nutrition tracker on the market today — MacroFactor, Bevel, MyFitnessPal, YAZIO, Nutracheck, Eat This Much — forces the same trade: hand your diet, weight, and eating-habit data to a cloud server, create an account, and pay a recurring subscription. Users with eating disorders, dietary restrictions tied to religion or health conditions, or simply a reasonable preference for privacy have no serious option. They either accept that their most personal data sits on someone else's server (where it can be breached, sold, or subpoenaed) or they go without.

Worse, the AI features that modern trackers ship are cloud-dependent: plate detection, natural-language logging, conversational coaching all round-trip user data to vendor servers. Privacy and AI have been treated as opposites.

Users want MacroFactor-grade nutrition tracking UX and Bevel-grade AI assistance, without the cloud, without the account, without the subscription.

## Solution

nutaru is a local-first, privacy-first nutrition tracker with an on-device intelligent local assistant. It ships for Android and iOS, works fully offline, never touches a server, and is funded as a free open-source project (AGPL v3).

The defensible wedge: every serious competitor is structurally locked into cloud + account + subscription. nutaru is structurally locked out of it. Privacy isn't a feature, it's the spine — proven by shipping genuinely useful on-device AI (so privacy doesn't mean capability compromise), backed by AGPL source (so the privacy claim is auditable), and free (so the wedge is open to everyone).

Key properties:

- **No account, no cloud, no telemetry.** App opens, works, full stop.
- **Encrypted at rest** with SQLCipher AES-256. Key derived from a 12-word recovery mnemonic the user controls (no server has it).
- **On-device assistant** powered by Gemma 4 E2B or E4B (multimodal — handles both text and plate-photo input in one model). Assistant is opt-in; users on capable devices can choose OS-provided models (Apple Foundation Models on iOS 26+) when capability probe passes.
- **Food data imported as user-chosen packs**, not bundled. App starts near-empty; user imports regional or brand packs sourced from Open Food Facts. No online API fallback — fully offline.
- **Assistant proposes, user confirms.** Every assistant write (log, edit, delete) goes through a suggest-then-confirm UI. Agent cannot auto-commit, even for plate-detect results. Batch confirmation supported for multi-item proposals.
- **Open source, AGPL v3.** Auditability is the credibility layer under the privacy claim.

## User Stories

### Onboarding

1. As a new user, I want to install nutaru without creating an account, so that I can start tracking immediately without giving up personal information.
2. As a new user, I want to set my profile basics (age, sex, height, current weight, goal, activity level), so that the app can calculate initial targets.
3. As a new user, I want to see my calculated daily targets (kcal + protein/carbs/fat), so that I can review them before starting.
4. As a new user, I want to manually override calculated targets, so that I can follow a plan prescribed by my coach or clinician.
5. As a new user, I want to see the reasoning behind my calculated targets, so that I trust the math (Mifflin-St Jeor + activity factor + goal delta).
6. As a new user, I want my device locale to suggest a relevant starter pack, so that I can install common local foods in one tap.
7. As a new user, I want to browse the pack catalog and choose what to install, so that I control what data lives on my device.
8. As a new user, I want to skip pack import and add later, so that I can start with manual entry if I don't want a download.
9. As a new user, I want to choose my assistant model at onboarding (Gemma 4 E2B, Gemma 4 E4B, OS model if available, or skip), so that I control device storage and capability tradeoffs.
10. As a new user, I want to skip assistant enablement and use nutrition-only mode, so that I can use the app without AI.
11. As a new user, I want my 12-word recovery mnemonic generated silently on first launch, so that all my data is encrypted from the first write.
12. As a new user, I want my mnemonic revealed at the end of onboarding after I've invested in setup, so that I'm most likely to pay attention and save it.
13. As a new user, I want to confirm I've saved the mnemonic by tapping a specific word, so that I can't accidentally skip saving it.
14. As a new user, I want a brief tutorial showing bottom navigation, Quick Add, bottom agent field, and day swipe, so that I understand the app's structure.
15. As a new user, I want to skip the tutorial, so that I can explore on my own.

### Privacy and data ownership

16. As a privacy-conscious user, I want my data stored only on my device, so that no server can leak, sell, or be compelled to disclose it.
17. As a privacy-conscious user, I want my database encrypted at rest with industry-standard AES-256, so that someone who physically obtains my phone cannot read my data.
18. As a privacy-conscious user, I want my encryption key derived from a 12-word mnemonic only I possess, so that no server holds it.
19. As a privacy-conscious user, I want zero telemetry and zero analytics, so that nothing about my usage leaves my device.
20. As a privacy-conscious user, I want to export an encrypted backup file at any time, so that I can store it where I choose.
21. As a privacy-conscious user, I want a single hard-delete action that wipes all tables and checkpoints the WAL, so that I can erase everything with no remnants.
22. As a privacy-conscious user, I want the source code available under AGPL v3, so that I can audit the privacy claims myself.
23. As a user changing phones, I want to restore from OS-level backup plus my mnemonic, so that my data carries over without a server.
24. As a user in a regulated jurisdiction (GDPR/CCPA), I want a published data-processing notice explaining all-on-device processing, so that I understand my rights.

### Food logging — input modes

25. As a tracker, I want a persistent Quick Add button beside the bottom nav, so that I can log from any screen with one tap.
26. As a tracker, I want the Quick Add bottom sheet to offer Text, Photo, Barcode, and Manual input modes, so that I can pick the fastest input for the situation.
27. As a tracker, I want to type a natural-language description ("2 eggs and toast"), so that the assistant parses and proposes entries for confirmation.
28. As a tracker, I want to photograph my plate, so that the assistant identifies items and portions for confirmation.
29. As a tracker, I want to combine text and photo ("chicken bowl" + photo), so that identification is more accurate than either alone.
30. As a tracker, I want to scan a barcode, so that packaged foods are looked up directly in installed packs.
31. As a tracker, I want to manually search by name and pick a product, so that I can log when the assistant is disabled or unsuitable.
32. As a tracker, I want to create a custom food item for home-cooked or restaurant meals, so that I can log foods not in any pack.
33. As a tracker, I want my custom foods searchable alongside pack foods, so that I can find them easily.

### Food logging — confirmation and editing

34. As a tracker, I want to confirm every proposed entry (manual, NL-parsed, plate-photo, barcode) before it commits, so that I retain final control of my diary.
35. As a tracker, I want to batch-confirm multiple proposed items with one tap, so that confirming a multi-item plate isn't tedious.
36. As a tracker, I want to see computed macros (kcal + P/C/F) update live as I edit quantity, so that I can spot mistakes before committing.
37. As a tracker, I want to log a food for a past or future date, so that I can backfill or pre-plan.
38. As a tracker, I want to tag entries by meal type (breakfast/lunch/dinner/snack), so that my diary organizes by meal.
39. As a tracker, I want to specify quantity in original units (1 cup, 2 slices, 100g), so that the entry matches how I think about portions.
40. As a tracker, I want to edit a committed entry, so that I can correct mistakes.
41. As a tracker, I want to delete a committed entry, so that I can remove accidental or wrong logs.
42. As a tracker, I want to log body weight from Quick Add, so that I don't have to navigate to a separate weight screen.

### Diary / day view

43. As a tracker, I want to see today's foods grouped by meal, so that I can review what I've eaten.
44. As a tracker, I want to swipe left/right to navigate days, so that I can review past or plan future intake.
45. As a tracker, I want a top date selector to jump to a specific date, so that I don't have to swipe through many days.
46. As a tracker, I want to see day-total macros against targets, so that I know how much I have left.
47. As a tracker, I want to see per-meal macro breakdowns, so that I understand distribution across the day.

### Dashboard / Home

48. As a tracker, I want a tile showing today's macro progress (kcal + P/C/F), so that I can quickly gauge where I am.
49. As a tracker, I want a tile showing my weight trend as a sparkline, so that I can see trajectory at a glance.
50. As a tracker, I want a tile with one actionable insight from the assistant, so that I get useful guidance without scrolling.
51. As a tracker, I want a tile showing recent log entries, so that I can jump back to recently used foods.
52. As a tracker, I want a tile showing weekly goal progress, so that I can see adherence over time.
53. As a tracker, I want to reorder dashboard tiles, so that I prioritize what matters to me.

### Assistant / agent

54. As a user, I want to chat with the assistant about my nutrition ("How's my protein trend this week?"), so that I get coaching and answers from my own data.
55. As a user, I want the assistant to propose logging when I describe a meal in the bottom text field, so that I log faster than manual search.
56. As a user, I want the assistant to propose edits and deletions ("you logged chicken twice — remove one?"), so that I keep my diary clean.
57. As a user, I want to confirm every assistant-proposed action before it commits, so that I always retain final control.
58. As a user, I want to see a text description of plate-photo results before confirming, so that I can verify what the assistant identified.
59. As a user who is blind or has low vision, I want the assistant to emit a text description of any plate-photo result, so that my screen reader can announce it.
60. As a user, I want to view past assistant conversations, so that I can refer back to previous insights.
61. As a user, I want to disable the assistant entirely in Settings, so that I can switch to nutrition-only mode if I change my mind.
62. As a user, I want to switch assistant model (E2B ↔ E4B ↔ OS) in Settings, so that I can change the storage/capability tradeoff later.

### Weight tracking

63. As a tracker, I want to log my body weight, so that the app tracks my progress over time.
64. As a tracker, I want the assistant to suggest logging weight on a cadence, so that I build the habit.
65. As a tracker, I want to see weight trend with smoothing, so that day-to-day noise doesn't mislead me.

### Targets

66. As a tracker, I want to see my current daily targets, so that I know what I'm aiming for.
67. As a tracker, I want to manually adjust targets, so that I can adapt to changing goals.

### Packs management

68. As a user, I want to browse the pack catalog, so that I can find packs relevant to me.
69. As a user, I want pack install to run as a background task, so that I can keep using the app during import.
70. As a user, I want to see pack import progress, so that I know when foods become searchable.
71. As a user, I want to uninstall a pack, so that I can reclaim storage.
72. As a user, I want to update a pack when a new version is available, so that I have current product data.
73. As a user, I want to import a side-loaded pack file, so that I can install packs not in the catalog.
74. As a user, I want to see license and attribution for each pack's data, so that I can verify provenance.

### Settings

75. As a user, I want to toggle between light, dark, and system themes, so that the app matches my preference.
76. As a user, I want to choose metric or imperial units for display, so that quantities match my convention.
77. As a user, I want to view my recovery mnemonic again in Settings, so that I can verify or re-save it.
78. As a user, I want to export an encrypted backup file, so that I can store it externally.
79. As a user, I want to view app version and license info, so that I can audit what's installed.

### Accessibility

80. As a screen reader user, I want every interactive element labeled, so that I can navigate the app via VoiceOver or TalkBack.
81. As a user with low vision, I want Dynamic Type / Font Scale to the system maximum, so that text remains readable at large sizes.
82. As a colorblind user, I want status indicators (over/under target) to use icon and text in addition to color, so that I can interpret them.
83. As a user sensitive to motion, I want animations to respect the system Reduce Motion setting, so that the app doesn't cause discomfort.
84. As a voice-control user, I want all operations operable via voice commands, so that I can use the app hands-free.

### Performance and offline

85. As an offline user, I want full app functionality with no internet connection, so that I can log foods on a plane or abroad without data.
86. As a user, I want the app to launch cold in under 2 seconds on reference hardware, so that logging a meal on the go is fast.
87. As a user on mid-tier hardware, I want the app to degrade gracefully, so that it remains usable even if the assistant is slow or disabled.

## Implementation Decisions

### Tech stack and architecture

- **Kotlin Multiplatform (KMP)** as the spine. Existing module layout is preserved and extended: `sharedLogic` (domain, storage, agent tool surface, pack import, target calc), `sharedUI` (Compose Multiplatform common UI, presenters, state machines, pure math), `androidApp` (Android entry point + thin platform wiring), `iosApp` (SwiftUI entry point + thin platform wiring). `webApp` (React/TS) stays in repo but is **not shipped** for V1.
- **iOS UI is SwiftUI-native**, consuming `sharedLogic` via KMP exports. `sharedUI` (Compose Multiplatform) is targeted at Android for V1; iOS adopts Compose Multiplatform only if a future decision unifies the UI layer. This split is already reflected in the repo and preserved.
- **Minimum OS versions**: Android API 28 (9.0)+; iOS 17+.
- **Hardware floor**: Devices with ≥6 GB RAM are eligible for the assistant; below this, the assistant is disabled at install-time detection and the app runs in nutrition-only mode. Apple Foundation Models gated to iOS 26+; below iOS 26 the Gemma 4 path is the only assistant option.
- **Reference hardware for performance budgets**: iPhone 17 Pro (high-end ceiling) and Pixel 5 (mid-tier floor, 8 GB RAM, Snapdragon 765G). Pixel 5 cannot use Gemini Nano (Pixel 8+ only) and is blocked from Gemma 4 E4B; it defaults to Gemma 4 E2B.

### Storage and schema

- **SQLDelight** as the single source of truth. Idiomatic KMP, type-safe SQL, integrates directly with SQLCipher.
- **SQLCipher AES-256** transparent DB encryption. No hand-rolled crypto.
- **Schema tables**:
  - `profile` (singleton) — age, sex, height, goal, activity level, created/updated timestamps.
  - `targets` — kcal + protein/carbs/fat per day, with `effective_from` for time-versioning. Source column distinguishes user-set vs algorithm-derived (algorithm-derived reserved for V1.1 adaptive targets).
  - `sources` — license, name, attribution text, url. Referenced by products.
  - `products` — food items (pack-derived and user-created in same table). Columns include id, barcode, name (i18n), brand, categories, nutrition per-100g, serving definitions (original unit + gram equivalent), ingredients, `source_id` foreign key, `pack_id` (nullable for user-created).
  - `packs` — installed pack metadata (id, name, version, source URL, imported_at, item count, attribution).
  - `log_entries` — food log. Columns include product_id, quantity (original numeric), unit (original string), quantity_grams (normalized for rollup math), meal type, source (manual/barcode/agent/recipe), timestamp, notes.
  - `weight_entries` — body weight log. Weight stored in kg (SI internal).
  - `agent_conversations`, `agent_messages`, `agent_proposals` — assistant chat history and the suggest-then-confirm queue. Each proposal carries op_type, op_payload as structured JSON, status (pending/confirmed/rejected), and resolved_at timestamp.
  - `agent_model_state` (singleton) — currently selected model id, on-disk path, byte size, downloaded_at, capability score from probe.
  - `settings` (singleton) — theme, locale, agent_enabled flag, units preference (metric/imperial).
  - `exercise_logs` — schema present at V1 as a forward-compatibility placeholder, UI hidden. Workout tab is a V2 placeholder.
  - `products_fts` — FTS5 virtual table backing product search.
- **Recipes are deferred to V1.1** (no schema, no UI).
- **Hard delete only** (no `deleted_at` soft-delete column). Privacy posture: user data is theirs; when deleted, it is gone with no shadow audit trail.
- **As-entered units stored alongside normalized grams** — original quantity and unit string preserved for display; `quantity_grams` column denormalized for fast macro rollup math.

### Encryption, key management, and recovery

- **Argon2id** derives the SQLCipher key from a 12-word recovery mnemonic (BIP-39 wordlist, sufficient entropy).
- **Mnemonic generated silently on app first launch**, before any user data is written. All onboarding data (profile, plan, pack imports, agent model download) is encrypted at rest with this key from the first write.
- **Mnemonic revealed to the user at onboarding step 5** (after Welcome+Profile → Plan → Pack → Agent). UX reasoning: user has invested in setup, more likely to attend and save carefully.
- **Confirmation gate**: user must tap a specific word from the mnemonic to proceed, proving they have seen and saved it.
- **If the user kills the app before completing step 5**, relaunch forces the reveal step first.
- **No online recovery path**. Lost mnemonic on a lost device = permanent data loss. This is the honest trade-off; copy must explain it plainly.

### Assistant / agent

- **Single multimodal model**: Gemma 4 E2B or E4B. One model covers both text (chat, NL parsing) and vision (plate detection) — no separate VLM needed.
- **Model choice at onboarding**:
  - "Download Gemma 4 E2B (~1.5 GB, faster, weaker)" — default recommendation on mid-tier Android (Pixel 5 class).
  - "Download Gemma 4 E4B (~3 GB, slower, stronger)" — recommended on flagship devices, blocked at install on Pixel 5 class.
  - "Use OS model (0 MB)" — shown only when the capability probe passes. On iOS 26+, this is Apple Foundation Models; on supported Android devices, Gemini Nano via AICore.
  - "Skip — nutrition only" — assistant disabled, all assistant UI hidden, re-enable in Settings.
- **Opt-in**. Assistant is never enabled without explicit user action.
- **Gemma 4 native function calling** for tool invocation. Tool schema is defined in `sharedLogic` commonMain, exposed to the platform model runtime, and the response parser maps tool-call results back to domain operations. Fail-forward to "I can't do that" if the model picks an undefined tool.
- **Fixed tool surface, no raw SQL**. Bounded tool set is the prompt-injection blast radius.
  - Read tools: `query_logs(dateRange, filters)`, `query_targets(date)`, `query_weight_history(dateRange)`, `aggregate_macros(dateRange, groupBy)`.
  - Write tools (all propose-only, never auto-commit): `log_food`, `log_weight`, `set_target`, `edit_entry`, `delete_entry`.
- **Suggest-then-confirm on every write**, including destructive operations. Each tool call returns a structured proposal rendered as a confirmation UI (bottom sheet) with a diff-style preview. Batch confirmation supported when a tool proposes multiple ops in one response (e.g., plate photo yields 5 items).
- **No embeddings or RAG**. User data is structured (SQL tables); SQL queries via tools beat embedding retrieval. Saves complexity and ~50–200 MB of embedding store.

### Food database and packs

- **Sources**: Open Food Facts (ODbL, attribution required) + user-created items stored locally. Same `products` table, distinguished by `source_id` foreign key.
- **Strict offline**. No online OFF API fallback. App starts near-empty; user imports packs they want.
- **Pack transport format = FlatBuffer**, length-prefixed product records, mmap-able, streamable for low-RAM devices. FlatBuffer is **only the transport format** — at import time all entries are parsed and inserted into the SQLDelight `products` table and the `products_fts` FTS5 index. Pack files are archived (for re-import) or discarded after successful import. SQLite is the single source of truth.
- **Pack distribution**: project-curated static catalog at `nutaru.app/catalog.json` (hosted on GitHub Pages free tier). Pack files hosted on GitHub Releases or Hugging Face dataset (free tier). No auth, no account, just HTTPS.
- **Initial catalog at V1 launch — 6 packs**: US, EU-mix (DE/FR/ES/IT), UK, JP, BR, plus global brands. Each pack 50–100k items, 25–50 MB. Built from OFF bulk export with curation by completeness and brand dominance (not scan_count alone, which skews EU-heavy).
- **Pack compiler tool** in `tools/pack-compiler/` — offline Kotlin utility that takes OFF bulk export as input and produces FlatBuffer pack files with embedded license/attribution per record.
- **Supply-chain integrity**: every pack entry in the catalog carries a SHA-256 checksum. App verifies checksum post-download. Gemma 4 model files are checksummed against Google's published hash.
- **Refresh cadence**: quarterly pack refreshes, versioned (semver), user-initiated re-import. No auto-update (preserves local-first).
- **Community pack-builder tool** deferred to V2.
- **Pack import runs as a background task** with progress indication; no strict latency budget. UI remains usable during import.

### Information architecture and UI

- **Bottom navigation bar**: Home / Diary / Workout / Settings.
  - Workout tab is a **V2 placeholder** at V1 (visible, shows coming-soon state).
- **Quick Add**: bottom action button beside the navigation bar (Bevel-style), opens a bottom sheet with input modes (Text / Photo / Barcode / Manual). Does not route through the assistant directly — assistant acts on the inputs after they're captured.
- **Bottom text field**: persistent assistant input (Bevel-style). Visible on Home, Diary, Workout (data tabs); hidden on Settings.
- **Top app bar**: date selector (Bevel-style). Visible on Diary and Workout only (date-context tabs).
- **Horizontal swipe**: previous/next day navigation within Diary and Workout.
- **Dashboard (Home) = tile/card based** (Google Health / Bevel style), not macro rings. Default tile set: today's macros, weight trend sparkline, agent insight, recent logs, weekly goal progress. User-reorderable.
- **Confirm-then-commit UI**: every assistant proposal (and any destructive manual action) renders as a bottom-sheet diff with confirm/reject. Batch confirm supported.

### Onboarding sequence

1. **Welcome + Profile** — privacy pitch explained, profile basics (age, sex, height, weight, goal, activity).
2. **Plan** — initial target calculation with reasoning shown, manual override available.
3. **Pack** — locale-suggested pack + catalog browse + import or skip.
4. **Agent** — assistant opt-in, model choice, download or skip.
5. **Recovery** — mnemonic reveal and confirm-saved gate.
6. **Home** — empty dashboard with "log your first meal" prompt.

### Privacy, legal, telemetry

- **No account, no sync, no telemetry, no analytics.** All four are absolute at V1.
- **Encrypted database stored in the OS-level backup slot** (iCloud Backup / Android Auto Backup). App does not drive its own cloud sync.
- **App Store privacy labels**: all categories marked "No" (no collection, no tracking, no linked data, no unlinked data). Strongest possible posture.
- **Play Console data safety form**: all zeros.
- **GDPR / data-processing notice** published at `nutaru.app/privacy`, explaining all-on-device processing, no data controller, no data processor. Required even without a server.
- **Right-to-erasure**: hard-delete-all action drops every table and checkpoints the WAL. No remnants.
- **Threat model**:
  - **Device lost/stolen**: SQLCipher AES-256 + Argon2id-derived key makes brute-force infeasible.
  - **Malicious assistant input**: bounded by fixed tool surface (no raw SQL), schema-validated tool args, user confirm gate on every op.
  - **Supply-chain tampering**: pack files checksummed (SHA-256 in catalog manifest), model files checksummed against Google's published hash, app binary signed by stores.
- **License: AGPL v3** for the entire codebase. Derivative network services must publish modifications.

### Performance budgets (NFRs)

| Metric | iPhone 17 Pro (ceiling) | Pixel 5 (floor) |
|---|---|---|
| App cold start | < 2 s | < 2 s |
| Food search (50k products, FTS5) | < 100 ms | < 100 ms |
| Agent first-token, warm | < 1 s | < 2 s |
| Agent first-token, cold | < 3 s | < 5 s |
| Agent streaming | ≥ 10 tok/s | ≥ 5 tok/s |
| Plate detect end-to-end | < 5 s | < 8 s |
| Log entry tap-to-commit | < 500 ms | < 500 ms |
| Pack import | Background, no budget | Background, no budget |

- Gemma 4 E4B blocked at install on Pixel 5-class hardware. Gemma 4 E2B is the default on mid-tier Android.
- Store copy sets expectation: "Best on 2023+ flagship Android; mid-tier supported in nutrition+assistant-lite mode."

### Localization

- **English-only at V1**. App strings, agent prompts, store copy, privacy doc — all English.
- **Pack content localization** is implicit: users import packs for their region, so product names and brands come localized via the data.
- **Localization to V1.1+** with priority locales TBD (likely DE/FR/ES/JA/ZH based on pack coverage).

### Branding

- **Product name: nutaru** (from repo path; confirmed).
- **External terminology**: "intelligent local assistant" (never "AI agent" in user-facing copy). Internally the codebase and this PRD use "assistant" or "agent" interchangeably.
- **Pitch one-liner**: *"Local-first nutrition tracking with an intelligent local assistant. No account. No cloud. No subscription. On your device. Yours."*

### Build, release, and CI

- **GitHub Actions** for CI. PR gate: build + `commonTest` + smoke NAE (10 queries) + `ktlint`/`detekt` + iOS build smoke. Main branch: full NAE + NPE + signed release builds.
- **Fastlane** for store submission (`fastlane supply` for Play, `fastlane deliver` for App Store).
- **Semver** — V1 ships as 1.0.0.
- **Signing keys** in 1Password (or equivalent hardware-backed secrets manager), never in repo.
- **Trunk-based development**, short-lived feature branches, squash-merge.
- **Solo developer pre-V1**; "PRs welcome" post-V1 once architecture stabilizes.
- **Tracer-bullet first**: ship a vertical slice ("log one food manually → SQLDelight write → tile dashboard reads it") before going wide on features. Proves schema, storage, and UI integration early.

### Hosting and funding

- **No monetization at V1**. No IAP, no donations infrastructure, no paid tier, no ads.
- **Free-tier hosting only**:
  - Catalog manifest → GitHub Pages.
  - Pack files → GitHub Releases or Hugging Face dataset (both free tier).
  - Gemma 4 model files → direct download from Google AI Edge CDN or Hugging Face Hub (zero bandwidth cost to project).
  - Privacy doc → GitHub Pages.
- **Project is pure cost** (developer time + free-tier infra). Funding model revisited at V1.1.

## Testing Decisions

### What makes a good test

- **Test external behavior, not implementation details.** A test should still pass when the internal implementation is refactored, as long as the observable behavior is unchanged.
- **One assertion per concept** where possible; tests should fail for one reason.
- **No I/O in unit tests** — databases, file systems, and networks are faked or stubbed at the seam.
- **Every non-trivial module ships at least one runnable check**, per the project's AGENTS.md. Trivial one-liners need no test. Non-trivial logic without a check is unfinished.
- **Golden-file and snapshot tests** are reserved for cases where the output is small, stable, and worth pinning (e.g., schema migrations, FTS5 query construction).

### Test seams (single dominant seam preferred)

1. **Primary seam: `sharedLogic:commonTest`** — the dominant seam, runs on JVM host in dev and on native targets (Android + iOS simulator) in CI. Houses all business logic tests: schema migration verification, SQLCipher open/encrypt/decrypt round-trips, mnemonic→key derivation (Argon2), target calculation (Mifflin-St Jeor + activity factors), pack FlatBuffer parse → SQLDelight bulk insert, food search (FTS5) query construction, agent tool surface (tool dispatch, arg schema validation, suggest-confirm op builders), log/weight/target CRUD operations. **If logic can live in `sharedLogic:commonMain`, it must.** One test here covers all platforms.

2. **Secondary seam: `sharedUI:commonTest`** — pure UI logic, no platform SDK calls. State machines and reducers (proposal-confirm flow, onboarding step transitions, dashboard tile ordering), pure math for rings/sparklines/tiles, view-state derivation from domain models (today's macro rollup, weight smoothing). Existing tests in this seam already follow the pattern (`ProgressRingMathTest`, `ProgressRingTest`, `ProgressPillTest`, `SharedUICommonTest`).

3. **Tertiary seam (new): NAE + NPE eval suites** — model-quality benchmarks, not unit tests. Live in a dedicated `eval/` Gradle module or `eval/` directory at repo root. Run nightly and pre-release, not per-commit (except for the smoke subset). Treat as a separate quality gate from logic correctness.

4. **Platform wrapper smoke checks** — minimal, in `androidMain`/`iosMain` source sets where unavoidable. Verify SQLCipher opens, model runtime loads, OS backup integration exists. These are not logic seams; platform code is kept thin (wrappers around interfaces defined in common, with fakes/stubs testable from `commonTest`).

### Modules tested

- `sharedLogic` — schema, encryption, target calc, pack import, food search, agent tool dispatch, CRUD operations. Dominant seam.
- `sharedUI` — state machines, reducers, pure math. Secondary seam.
- `eval` — Nutaru Agent Eval (NAE), Nutaru Plate Eval (NPE). Tertiary seam.

### Nutaru Agent Eval (NAE)

- **50 hand-authored golden queries** covering categories: `log_food`, `log_weight`, `query_macros`, `query_history`, `set_target`, `suggest_delete`, natural-language parse, plate-caption.
- **Three grading facets per query**: tool selection (binary — correct tool called), tool args valid (binary — passes schema), response sensible (manual 1–5 rating, ≥ 3 passes).
- **Floor**: ≥ 85% pass overall (≥ 42/50). 90% aspirational.
- **Smoke subset**: 10 queries run in CI per PR (must pass for green build). Full 50 runs nightly and pre-release.
- **Versioning**: queries and expected outputs are versioned alongside model versions to track regressions across model upgrades.

### Nutaru Plate Eval (NPE)

- **50 manually-labeled plate photos**, mixed: simple (1–2 items), typical meals (3–5), complex (6+).
- **Cuisines**: Western, Asian, Latin American, Middle Eastern (matches launch pack regions).
- **Grading**: top-3 item precision (predicted vs labeled) — primary metric; portion accuracy within ±30% (informational only at V1).
- **Floor**: ≥ 65% top-3 item precision on the full 50. 70% aspirational.
- **Held-out set**: 10 images reserved as the "live" gate, never used during dev iteration. Final ship decision runs against this set.

### Beta testing gate

- Closed beta via TestFlight and Play Internal Testing.
- Cohort size 20–50 users, recruited from r/QuantifiedSelf, r/privacy, HN, Mastodon fediverse, with Discord for feedback loop.
- 2–4 week feedback window; need ≥ 10 active testers giving feedback to ship.
- **Ship-blockers**: P0 crash in > 1% of sessions, crash-free session rate below 99% over the beta window, NAE below 85%, NPE below 65%.

### Accessibility audit

- **WCAG 2.1 AA across all dimensions** is a ship-blocker.
- Run Accessibility Inspector (iOS) and Accessibility Scanner (Android) on every screen pre-beta.
- Fix all P0 and P1 findings before beta cutoff. P2 findings tracked as V1.1 backlog.
- Verify contrast in light, dark, and system themes.
- Verify every assistant proposal renders a text description (for screen-reader users on plate-detect results).

### Prior art in the codebase

- `sharedLogic:commonTest` — `SharedLogicCommonTest` and per-platform variants already follow the dominant-seam pattern.
- `sharedUI:commonTest` — `ProgressRingMathTest`, `ProgressRingTest`, `ProgressPillTest`, `SharedUICommonTest` demonstrate pure-UI-logic testing without platform dependencies. New tests follow this convention.

## Out of Scope

The following are explicitly excluded from V1 and reserved for V1.1 or later. Tracking them here prevents scope creep.

- **Health metrics ingest** (sleep, HR, biometrics from Apple Health / Health Connect). Bevel's holistic-health feature set is inspiration only.
- **Adaptive / algorithmic target adjustment** (MacroFactor's signature feature). V1 ships static user-set targets with manual override. Adaptive algorithm lands in V1.1 with proper math and a transparency UI.
- **Recipe builder** (combining foods into a recipe with servings). Schema deferred; UI deferred.
- **Workout / training log** UI. Workout tab is a visible V2 placeholder; `exercise_logs` table is in the schema as forward-compatibility.
- **Cloud sync** of any kind. No iCloud Drive, no Google Drive, no self-host server, no CRDT layer. OS-level backup only.
- **Telemetry and analytics** (even opt-in crash reporting).
- **Account** of any kind (no signup, no email, no profile on a server).
- **Community pack-builder tool**. V1 ships project-curated packs only; community tool is V2.
- **Monetization** of any kind. No IAP, no subscriptions, no ads, no donations infrastructure.
- **Localization beyond English**. App strings, agent prompts, store copy all English at V1.
- **Web app** (the React/TS `webApp` in the repo) is not shipped as a V1 product. KMP target kept alive for future option.
- **Multi-device use**. Single-device at V1.
- **Apple Foundation Models on iOS < 26**. Below iOS 26, the Gemma 4 path is the only assistant option.
- **Embeddings / RAG**. Structured data is queried via fixed tool surface; no vector store.

## Further Notes

### Risk register

Top risks tracked from the design grilling, to be maintained in `docs/risks.md` and re-graded quarterly:

1. **On-device assistant capability ceiling on mid-tier hardware (Pixel 5 class)** — mitigated by split perf budgets, E2B-only default on mid-tier, E4B install block on weak devices, OS-model path on iOS 26+.
2. **Plate-detect accuracy (~65% top-3)** — noticeable failure rate; mitigated by frictionless confirm UI, retry, manual edit, and assistant-generated text description.
3. **Mnemonic loss = permanent data loss** — silent generate at first launch, reveal at peak-attention onboarding step 5, repeated reminders in Settings, monthly export reminder.
4. **Open Food Facts data quality uneven** (EU-heavy, US thin, crowdsourced errors) — pack curation by completeness and brand dominance, manual entry always available, community pack-builder V2.
5. **Empty app at first launch** (no packs = no foods) — locale-based suggestion in onboarding, one-tap import, manual entry always works.
6. **Solo developer scope = schedule slip risk** — tracer-bullet first, strict CI, aggressive MVP cuts.
7. **Assistant prompt-injection / malicious input** — bounded by fixed tool surface (no raw SQL), suggest-confirm gate, schema-validated tool args.
8. **Supply-chain tampering** (pack files, model files) — SHA-256 checksums in catalog manifest, model checksums against Google's published hash, store-signed binary.
9. **Pixel 5 hardware ceiling = mid-tier UX degradation** — store copy sets expectation, E2B-only, relaxed budgets.
10. **Beta cohort recruitment** (20–50 privacy/QS users needed) — r/QuantifiedSelf, r/privacy, HN, Mastodon, Discord.
11. **Apple Foundation Models gated to iOS 26+** — clear onboarding UX, document iOS version requirement, Gemma 4 fallback universal.
12. **App Store privacy review despite "all No" labels** — GDPR doc at `nutaru.app/privacy`, fast response workflow ready.
13. **Compose Multiplatform accessibility semantic leaks** — hand-audit both platforms per screen, override in expect/actual where needed.
14. **Beta→launch quality bar slippage (NAE/NPE thresholds)** — CI smoke gate nightly, pre-release full eval, ship-blocker if P0 crash > 1%.

### Differentiator

Spine: **privacy-first**. Proof: on-device assistant. Credibility: AGPL v3 source. Bonus: free forever. Lead with privacy (the defensible wedge competitors structurally cannot follow without rewriting their business), prove it with the assistant (privacy without capability compromise), back it with AGPL (auditable), and free removes the price barrier.

One-liner: *"Local-first nutrition tracking with an intelligent local assistant. No account. No cloud. No subscription. On your device. Yours."*

### Competitive landscape reference

- **MacroFactor** — best-in-class nutrition logging UX + adaptive algorithm. Cloud, account, ~$80/yr subscription. nutaru borrows the *UX patterns* (logging flow, target presentation, weekly review shape), not the algorithm.
- **Bevel** — best-in-class agent UX (chat-first, insight cards, polished transitions) + holistic health scope. Cloud, account, subscription. nutaru borrows the *UX patterns* (bottom text field, tile dashboard, onboarding grace), not the holistic feature set.
- **MyFitnessPal, YAZIO, Nutracheck, Eat This Much** — all cloud + account + subscription. None privacy-first, none on-device AI.

nutaru's structural wedge: every competitor's business model requires cloud + account + subscription. nutaru's business model (free, AGPL, donation-funded OSS) structurally cannot use cloud + account. The wedge is the business model, not a feature.

### Forward references

- `docs/risks.md` — risk register (to be created from the risk list above).
- `docs/pack-compiler-spec.md` — FlatBuffer pack format spec (to be created before pack-compiler implementation).
- `docs/agent-tool-surface-spec.md` — formal tool schema definitions (to be created before agent integration implementation).
- `docs/privacy.md` — published privacy/GDPR notice (to be created and hosted at `nutaru.app/privacy`).
