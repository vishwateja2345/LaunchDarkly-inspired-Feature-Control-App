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
