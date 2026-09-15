---
name: rollout-bucketing-check
description: Statistically verify that FlagDeck's percentage-rollout bucketing is stable (same user always gets the same variation) and distributed close to the configured weights. Use after changing the evaluation engine or rollout math, or before a demo, to confirm the rollout feature behaves correctly against a running backend.
---

# Rollout Bucketing Check

FlagDeck's progressive-rollout feature depends on one core guarantee: for a given flag,
environment, and user key, the variation returned by `POST /api/v1/evaluate` must always be the
same (stickiness), and across many distinct users the split must approximate the configured
percentages (fairness). This skill exercises that guarantee against a running backend instead
of trusting the implementation by inspection alone.

## When to use this

- After changing anything in `backend/src/main/java/com/featureflags/flags/Bucketing.java` or
  `FlagEvaluator.java`.
- After changing a flag's rollout weights and wanting quick confidence before a demo or review.
- As a lightweight regression check alongside `skills/validate`.

## Prerequisites

- The backend is running and reachable (default `http://localhost:8000`, override with
  `--api-base`).
- A login for any seeded account (defaults to `alex.morgan@flagdeck.com` /
  `password123` - every API route except health and login requires a Bearer token).
- A project, environment, and a boolean or multivariate flag with a `fallthroughRollout`
  configured (the seeded `new-checkout-flow` flag in Production, at a 25/75 split, works out of
  the box).

## Running it

```bash
python3 skills/rollout-bucketing-check/check_rollout.py \
  --project-id <projectId> --flag-key new-checkout-flow --environment-key production \
  --samples 2000
```

Add `--api-base http://localhost:8000/api/v1` to point at a non-default backend.

## What it checks and reports

1. **Stickiness** - evaluates the same 25 user keys twice each and fails loudly if any user's
   variation changed between calls.
2. **Distribution** - evaluates `--samples` distinct synthetic user keys once each, tallies the
   returned variation values, and prints the observed percentage per variation next to the
   configured weight from the flag's `fallthroughRollout` (fetched from
   `GET /flags/{flagId}/environments/{environmentKey}/config` using the flag lookup from the
   project's flag list).
3. **Tolerance** - flags a `WARN` if any variation's observed share is more than 5 percentage
   points away from its configured weight (expected to shrink towards 0 as `--samples` grows;
   2,000+ samples should comfortably stay within tolerance for weights above ~10%).

This is a read-only script: it only logs in and calls `/evaluate` and two `GET` routes to read
the flag's configured weights. It never creates, modifies, or deletes a flag, environment, or
approval.
