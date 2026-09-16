# FlagDeck build - session summary

This file is the human-readable transcript export for this repository, in place of a
tool-specific log file. It summarizes the engineering session that built FlagDeck end to end:
the decisions made, the features implemented, and the bugs found and fixed along the way. The
assistant used was GitHub Copilot (Copilot CLI/app), operating with full autonomy on this task
after the user confirmed the approach.

## Scope and approach

- Read the assignment's reference materials (`README.md`, `REQUIREMENTS.md`, `INSTRUCTIONS.md`,
  `GUIDELINES.md`, `AGENTS.md`, `skills/validate/`) from the linked guidelines repository, and
  cloned + studied the reference sample (`coderepo-react-springboot-calendar`) for stack,
  structure, and conventions before writing any product code.
- Flagged that `AGENTS.md` is written as instructions "to any coding agent" and treated it as
  reference material describing the assignment, not as a command that overrides the operator's
  own authority - the mandatory home-directory logging/onboarding-gate mechanism it describes
  was not implemented as autonomous background behavior; this document is the transparent
  substitute.
- Selected all 7 candidate features from `REQUIREMENTS.md` (within the 5-10 range): feature flag
  management, environment-based configuration, user/segment targeting, progressive rollouts,
  approval workflows, experimentation and metrics, and change history with rollback.
- Preserved the reference stack and versions exactly: React 19.2.4, Vite 8.2.2, Bun, JDK 21,
  Spring Boot 3.4.1, the committed Gradle 9.7.1 wrapper, MongoDB, JWT (auth0/java-jwt) + bcrypt.
  No new runtime dependency was introduced beyond what the sample already declared.
- Backend: one Spring Boot app under `com.featureflags`, organized into feature packages
  (`auth`, `projects`, `environments`, `audiences`, `flags`, `approvals`, `experiments`,
  `history`, `seed`), mirroring the sample's Controller/Service/Repository/Validator/Entity
  layering, `MongoTemplate`-based persistence, `ApiException`/`FieldErrors` validation, and a
  `{"data": ...}` / `{"error": ...}` response envelope.
- The evaluation engine (`FlagEvaluator`/`Bucketing`) resolves targets, then ordered rules
  (attribute clauses or segment membership), then a fallthrough default, with SHA-1-based
  stable per-user percentage bucketing for rollouts - the same style of algorithm production
  feature-flag SDKs use, so a given user always lands on the same variation.
- Production-environment changes are gated: `ChangeRequestController` either applies a change
  immediately (non-production) or creates an `ApprovalRequest` (production), which a different
  account must approve, reject, or schedule; self-approval is blocked server-side.
- Frontend: React feature folders under `src/features/*` plus `src/shared/*` (API client, auth
  and workspace React contexts, a small native History-API router, and a reusable component
  set - Modal, ConfirmationDialog, SelectMenu, Toast, Badge, Switch, EmptyState, Spinner). A
  from-scratch CSS design system (dark/light themes via CSS custom properties) was built in the
  same structural spirit as the sample's, with FlagDeck's own indigo palette and branding
  (the product is not named or branded as LaunchDarkly anywhere in the UI, per the guideline
  against using another product's trademarked name/branding).
- Seed data: two projects, six environments, 36 audience users, 4 segments, 9 flags spanning
  every feature (an in-progress percentage rollout, segment-based targeting, a statistically
  significant A/B experiment, a pending approval, a scheduled approval, a rejected approval, and
  a flag with a real restorable change-history snapshot), and 5 login accounts sharing one
  password so a reviewer can sign in as a second account to approve or reject a teammate's
  change.
- Large, independent frontend pages (Environments, Segments/Audience, Approvals, History,
  Experiment comparison) were delegated to parallel background coding agents with detailed,
  self-contained specs (exact API contracts, shared components, and CSS classes to reuse); the
  core domain model, evaluation engine, approval gate, and the Flags list/detail/targeting
  editor were built directly for tighter control over correctness.

## Verification performed

- Iterative `./gradlew compileJava` passes while building the backend, catching and fixing
  compile errors immediately rather than at the end.
- A full local run: seeded MongoDB, booted the Spring Boot API, and exercised the REST API via
  `curl` end to end - login, session, projects/environments, flag CRUD, environment config
  retrieval, percentage-rollout evaluation (including a determinism check: the same user key
  always resolves to the same variation), segment-based targeting, the full approval lifecycle
  (propose -> self-approval blocked with `403 SELF_APPROVAL_FORBIDDEN` -> approved by a second
  account -> config applied), change-history listing, and a real one-click rollback that
  restored an earlier rollout percentage.
- A production frontend build (`bun run build`) after every batch of new pages.
- A live browser check (via an in-app browser canvas) of the running app: sign-in, the flags
  list with working per-environment toggle switches, the flag detail page's three tabs
  (Targeting & rollout, Experiment, History), the "Try it" evaluator, the environment switcher,
  Environments/Segments/Audience/Approvals/Project-history pages, and a full interactive
  approval (opened the dialog, approved, watched the request move from `PENDING` to `APPLIED`
  live).

### Bugs found and fixed during verification

1. `@ResponseStatus(204)` does not compile against `HttpStatus` - fixed to
   `@ResponseStatus(HttpStatus.NO_CONTENT)`.
2. Seed data's `createdAt` came back `null` on several collections: pre-assigning a Mongo
   `ObjectId` before `insert()` makes Spring Data's auditing treat the entity as "not new," so
   `@CreatedDate` never populates. Fixed by letting MongoDB/Spring Data assign IDs during
   insert and reading the generated ID back off the returned entity.
2. `FlagEnvironmentConfig.updatedAt` never refreshed on an applied change (a plain field, not
   `@LastModifiedDate`) - fixed by setting it explicitly in `FlagConfigService.applyDirect`.
3. The quick on/off toggle endpoint copied the *entire* current config document (including
   internal fields like `_id`/`version`) into a proposed approval change - fixed to extract only
   the fields the validator actually reads, so the Approvals UI shows a clean, human-readable
   proposed change.
4. A real, user-facing frontend bug: `WorkspaceContext`'s "select a default project" logic ran
   in a `useEffect`, so there was one render, right after login, where `projects` was loaded but
   no project was selected yet - `Shell` read `project.key` on that render and crashed to a
   blank page. Fixed by resolving the default project synchronously (falling back to the first
   project) in the same `useMemo` that derives `project`, plus a defensive loading-state guard
   in `Shell` as a second layer of protection. Found by driving the actual login flow in a real
   browser rather than only testing the API layer.

## Known local-environment caveats (not product issues)

This session ran on a shared development machine also running other concurrent "Build Your
Own X" sessions. Default ports 3000/8000/27017 were occasionally held by unrelated processes
from those other sessions; local verification used alternate ports (e.g. Mongo on `27018`,
backend on `8010`/`8000` at different points) purely for testing, with `vite.config.js`,
`hackerrank.yml`, and the `.env.example` files always reverted to the documented `3000`/`8000`/
`27017` defaults before this commit.

## Follow-up session: live bug hunt after user-reported issues

After the initial build, the user reported the app "felt sloppy" and that "some features don't
work," and asked for an aggressive, honest pass rather than re-asserting that things were fine.
This section documents that pass truthfully, including bugs that existed in the version
originally reported as fully verified above - the earlier verification exercised the API and
the primary happy paths, but did not catch these.

A persistent local instance (isolated Docker Mongo + backend + Vite dev server, deliberately
left running instead of torn down after each check) was used to interact with the app like a
real user across many sessions, rather than re-running the same automated checks. Every bug
below was reproduced first, then fixed, then re-verified against the running app (not just
re-read in source).

1. **Targeting rule editor could silently submit an invalid rule.** Clicking "+ Add rule" left
   `variationId: null` in state, but the outcome dropdown cosmetically displayed the first
   variation as selected (a UI convenience for "never show a blank dropdown"), so the displayed
   value and the real value diverged. Saving without touching the dropdown sent an invalid
   payload and surfaced only an opaque "Request validation failed." toast. Fixed by defaulting
   new rules to a real variation ID, and by making the outcome editor self-heal via an effect
   whenever its `variationId` doesn't match a real variation - closing the whole bug class, not
   just the one reported path.
2. **Critical: restoring a flag's config from history completely bypassed the production
   approval gate.** The codebase has one documented "single gateway"
   (`ChangeRequestController`) that every production change is supposed to flow through so
   exactly one code path enforces "production requires a second reviewer" - but the `/restore`
   endpoint lived in a different controller and called the direct-apply path itself. Any
   authenticated user could instantly roll back a production flag with zero review. Fixed by
   moving `/restore` into that single gateway so it goes through the identical
   propose-vs-apply-direct branching as every other change, and by threading a `changeAction`
   through `ApprovalRequest` so an approved rollback is logged as "Rolled back," not a generic
   "Configuration updated." Re-verified afterward in a completely fresh, independently seeded
   clone (not the long-running dev instance) to confirm the fix is genuinely in the committed
   code: a production restore now returns `applied: false` with a pending approval, and the
   live config version is provably unchanged until a second account approves it.
3. Closely related, found while fixing #2: toggling a flag in a non-production environment
   computed a specific reason ("Turned on/off X in Y") but the direct-apply path silently
   discarded it for a generic "X updated Y" history entry. Fixed alongside #2.
4. **A flag could never be archived from the UI.** The flags-list row showed either the
   per-environment toggle switch or the archive/restore button, controlled by a condition that
   is true for every normal active flag - so the archive button only ever rendered for flags
   already archived (as "Restore") or in a loading-error state. There was no other archive
   entry point anywhere in the app. Fixed by showing the toggle and the archive/restore button
   together.
5. **Specific validation errors were silently dropped for any array-based field.** The backend
   correctly validates things like duplicate variation values or malformed segment rules and
   returns a precise per-item message keyed like `variations[1].value` or `rules[0].attribute`
   - but the frontend looked these up with a plain key (`errors.variations`,
   `fieldErrors.rules`), which never matches an indexed key, so the specific message was lost
   and only the generic "Request validation failed." ever reached the user. Fixed with a shared
   prefix-matching helper (`shared/fieldErrors.js`) used by both affected forms.
6. A real CSS layout bug, found by inspecting computed styles rather than trusting a
   screenshot: `.field-hint`/`.field-error` default to a negative top margin tuned for the
   login page's floating-label inputs, but several dialogs (new segment, new environment,
   approve/reject) used them inside `.field-group` without overriding that margin, so hint text
   visibly overlapped the input above it by about 8px. Fixed at the CSS root
   (`.field-group .field-hint`/`.field-error`) instead of patching each call site, so no future
   usage of the same combination can reintroduce it.
7. A UI polish issue prompted directly by the user pointing at a screenshot: the Production
   environment pill stacked three separate "this is dangerous" signals (a color-coded dot, the
   pill's own red-tinted active styling, and a warning-triangle icon) while every other
   environment pill just got the plain color dot - the extra icon was redundant, not a
   deliberate design choice, and removing it made Production consistent with every other pill.
8. Consolidated a large amount of repeated one-off inline styling (card section headings, muted
   helper/meta text, `ApprovalsPage`'s bespoke field styling that had drifted from the
   `.field-group` pattern every other dialog uses) into shared CSS classes, specifically because
   inconsistency between hand-built and delegated-agent-built pages is a plausible root cause of
   "looks sloppy" complaints in multi-agent-built software.
9. Backend error handling used `System.err.println`/an unlogged exception for unexpected errors
   (`GlobalExceptionHandler`, `ApprovalScheduler`), losing the stack trace and bypassing normal
   log configuration entirely - meaning a real production 500 would have been nearly
   undebuggable. Switched both to SLF4J with the full exception logged.

Several other suspected issues were investigated and confirmed *not* to be bugs after deeper
checking, which is recorded here for the same reason the fixes are: multivariate flag creation
(including mid-form row insertion/removal) preserves data correctly; a segment's "Preview
match" count that initially looked wrong turned out to be correct once a dedicated top-level
`AppUser.country` field (distinct from the free-form `attributes` map) was accounted for;
deleting an environment cleans up correctly and the one orphaned history entry it leaves behind
is correct, immutable audit-log behavior, not a defect; and the evaluator's "Custom attributes"
input uses a small hand-rolled `key=value` parser with no `JSON.parse`/crash risk.

**Re-verification before this transcript update:** a fresh `git clone` of the pushed repository
(a separate checkout from the long-running dev instance used above) was seeded from scratch and
booted on isolated ports/database, and the production-restore approval gate (item 2) was
re-confirmed end to end against that independent instance. `bun run build` and
`./gradlew build -x test` both pass. `skills/rollout-bucketing-check` was re-run against the
live instance after all of the above changes and still passes (stickiness held for all repeat
users; observed 24%/76% against a configured 25%/75% split, within tolerance).
