import { useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { flagsApi } from "../flags/flags.api.js";

const REASON_LABEL = {
    FLAG_OFF: "Flag is off",
    TARGET_MATCH: "Matched an individual target",
    RULE_MATCH: "Matched a targeting rule",
    FALLTHROUGH: "Fell through to the default rule",
    ERROR: "Could not resolve a variation",
};

function parseAttributes(text) {
    const attributes = {};
    text.split(",").forEach((pair) => {
        const [key, ...rest] = pair.split("=");
        const value = rest.join("=").trim();
        if (key && key.trim() && value !== "") {
            if (value === "true" || value === "false") attributes[key.trim()] = value === "true";
            else if (!Number.isNaN(Number(value))) attributes[key.trim()] = Number(value);
            else attributes[key.trim()] = value;
        }
    });
    return attributes;
}

export function EvaluatorPanel({ flag, projectId, environmentKey }) {
    const [userKey, setUserKey] = useState("test-user-1");
    const [plan, setPlan] = useState("");
    const [country, setCountry] = useState("");
    const [attributesText, setAttributesText] = useState("");
    const [result, setResult] = useState(null);
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);

    const submit = async (event) => {
        event.preventDefault();
        setBusy(true);
        setError("");
        setResult(null);
        try {
            const data = await flagsApi.evaluate({
                projectId,
                flagKey: flag.key,
                environmentKey,
                user: {
                    key: userKey.trim(),
                    plan: plan.trim() || undefined,
                    country: country.trim() || undefined,
                    attributes: parseAttributes(attributesText),
                },
            });
            setResult(data);
        } catch (caught) {
            setError(caught.message || "Unable to evaluate this flag.");
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="card">
            <h3>
                <MaterialIcon size={18}>bolt</MaterialIcon> Try it
            </h3>
            <p className="card-subtitle">
                Simulate an SDK evaluation for a user without leaving this page.
            </p>
            <form onSubmit={submit}>
                <div className="field-row">
                    <div className="field-group">
                        <label htmlFor="evaluator-key">User key</label>
                        <input id="evaluator-key" required value={userKey} onChange={(event) => setUserKey(event.target.value)} />
                    </div>
                    <div className="field-group">
                        <label htmlFor="evaluator-plan">Plan</label>
                        <input id="evaluator-plan" placeholder="free / pro / enterprise" value={plan} onChange={(event) => setPlan(event.target.value)} />
                    </div>
                    <div className="field-group">
                        <label htmlFor="evaluator-country">Country</label>
                        <input id="evaluator-country" placeholder="US" value={country} onChange={(event) => setCountry(event.target.value)} />
                    </div>
                </div>
                <div className="field-group">
                    <label htmlFor="evaluator-attributes">Custom attributes</label>
                    <input
                        id="evaluator-attributes"
                        placeholder="betaOptIn=true, signupCohort=2026-q1"
                        value={attributesText}
                        onChange={(event) => setAttributesText(event.target.value)}
                    />
                </div>
                <button className="secondary-button" disabled={busy} type="submit">
                    {busy ? "Evaluating…" : "Evaluate"}
                </button>
            </form>

            {error && (
                <p className="inline-error" style={{ marginTop: 12 }}>
                    {error}
                </p>
            )}

            {result && (
                <div style={{ marginTop: 14, display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
                    <Badge tone={result.reason === "FLAG_OFF" ? "neutral" : "success"}>{result.value ?? "no value"}</Badge>
                    <span style={{ fontSize: 13, color: "var(--muted)" }}>{REASON_LABEL[result.reason] || result.reason}</span>
                </div>
            )}
        </div>
    );
}
