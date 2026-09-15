#!/usr/bin/env python3
"""Verify FlagDeck's rollout bucketing is sticky and fairly distributed.

See ../SKILL.md for context. Read-only: only calls POST /evaluate.
"""
import argparse
import json
import sys
import urllib.request
from collections import Counter


def login(api_base, email, password):
    body = json.dumps({"email": email, "password": password}).encode("utf-8")
    request = urllib.request.Request(
        f"{api_base}/auth/login", data=body, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        payload = json.load(response)
    return payload["data"]["token"]


def evaluate(api_base, token, project_id, flag_key, environment_key, user_key):
    body = json.dumps({
        "projectId": project_id,
        "flagKey": flag_key,
        "environmentKey": environment_key,
        "user": {"key": user_key},
    }).encode("utf-8")
    request = urllib.request.Request(
        f"{api_base}/evaluate",
        data=body,
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {token}"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        payload = json.load(response)
    return payload["data"]


def fetch_rollout_weights(api_base, token, project_id, flag_key, environment_key):
    auth_header = {"Authorization": f"Bearer {token}"}
    request = urllib.request.Request(f"{api_base}/projects/{project_id}/flags", headers=auth_header)
    with urllib.request.urlopen(request, timeout=10) as response:
        flags = json.load(response)["data"]
    flag = next((candidate for candidate in flags if candidate["key"] == flag_key), None)
    if flag is None:
        raise SystemExit(f"No flag with key '{flag_key}' in project {project_id}")

    config_request = urllib.request.Request(
        f"{api_base}/flags/{flag['_id']}/environments/{environment_key}/config", headers=auth_header
    )
    with urllib.request.urlopen(config_request, timeout=10) as response:
        config = json.load(response)["data"]

    values_by_id = {variation["id"]: variation["value"] for variation in flag["variations"]}
    rollout = config.get("fallthroughRollout") or []
    return {values_by_id.get(entry["variationId"], entry["variationId"]): entry["weight"] for entry in rollout}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-base", default="http://localhost:8000/api/v1")
    parser.add_argument("--email", default="alex.morgan@flagdeck.com")
    parser.add_argument("--password", default="password123")
    parser.add_argument("--project-id", required=True)
    parser.add_argument("--flag-key", required=True)
    parser.add_argument("--environment-key", required=True)
    parser.add_argument("--samples", type=int, default=2000)
    parser.add_argument("--tolerance", type=float, default=5.0, help="Percentage-point tolerance before WARN")
    args = parser.parse_args()

    token = login(args.api_base, args.email, args.password)

    print(f"Checking stickiness for 25 repeat users...")
    stickiness_failures = 0
    for index in range(25):
        user_key = f"bucketing-check-sticky-{index}"
        first = evaluate(args.api_base, token, args.project_id, args.flag_key, args.environment_key, user_key)
        second = evaluate(args.api_base, token, args.project_id, args.flag_key, args.environment_key, user_key)
        if first["variationId"] != second["variationId"]:
            stickiness_failures += 1
            print(f"  FAIL: {user_key} got {first['variationId']} then {second['variationId']}")

    if stickiness_failures:
        print(f"Stickiness: FAIL ({stickiness_failures}/25 users changed variation on repeat evaluation)")
    else:
        print("Stickiness: PASS (all repeat evaluations were stable)")

    print(f"\nSampling {args.samples} distinct users for distribution...")
    tally = Counter()
    for index in range(args.samples):
        result = evaluate(
            args.api_base, token, args.project_id, args.flag_key, args.environment_key, f"bucketing-check-{index}"
        )
        tally[result["value"]] += 1

    configured = fetch_rollout_weights(args.api_base, token, args.project_id, args.flag_key, args.environment_key)

    print(f"\n{'Variation':<20}{'Configured %':<16}{'Observed %':<14}Status")
    exit_code = 0
    for value, weight in configured.items():
        observed = (tally.get(value, 0) / args.samples) * 100 if args.samples else 0
        delta = abs(observed - weight)
        status = "OK" if delta <= args.tolerance else "WARN (outside tolerance)"
        if delta > args.tolerance:
            exit_code = 1
        print(f"{value:<20}{weight:<16.1f}{observed:<14.1f}{status}")

    if stickiness_failures:
        exit_code = 1

    sys.exit(exit_code)


if __name__ == "__main__":
    main()
