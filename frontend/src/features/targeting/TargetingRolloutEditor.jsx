import { useEffect, useMemo, useState } from "react";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { Switch } from "../../shared/components/Switch.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { useToast } from "../../shared/components/Toast.jsx";
import { flagsApi } from "../flags/flags.api.js";
import { segmentsApi } from "./targeting.api.js";
import { approvalsApi } from "../approvals/approvals.api.js";
import { OutcomeEditor } from "./OutcomeEditor.jsx";
import { RuleEditor } from "./RuleEditor.jsx";
import { EvaluatorPanel } from "./EvaluatorPanel.jsx";

function targetsToRows(targets, variations) {
    const rows = new Map(variations.map((variation) => [variation.id, ""]));
    (targets || []).forEach((target) => rows.set(target.variationId, (target.userKeys || []).join(", ")));
    return rows;
}

function newRule(variations) {
    return {
        id: `rule-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        description: "",
        clauses: [{ attribute: "", operator: "in", values: [] }],
        variationId: variations[0]?.id ?? null,
        rollout: [],
    };
}

export function TargetingRolloutEditor({ config, environment, flag, projectId, onSaved }) {
    const { show } = useToast();
    const [enabled, setEnabled] = useState(config.enabled);
    const [offVariationId, setOffVariationId] = useState(config.offVariationId);
    const [targetRows, setTargetRows] = useState(() => targetsToRows(config.targets, flag.variations));
    const [rules, setRules] = useState(config.rules?.length ? config.rules : []);
    const [fallthrough, setFallthrough] = useState({
        variationId: config.fallthroughVariationId,
        rollout: config.fallthroughRollout || [],
    });
    const [segments, setSegments] = useState([]);
    const [pendingApproval, setPendingApproval] = useState(null);
    const [saving, setSaving] = useState(false);
    const [dirty, setDirty] = useState(false);

    useEffect(() => {
        setEnabled(config.enabled);
        setOffVariationId(config.offVariationId);
        setTargetRows(targetsToRows(config.targets, flag.variations));
        setRules(config.rules?.length ? config.rules : []);
        setFallthrough({ variationId: config.fallthroughVariationId, rollout: config.fallthroughRollout || [] });
        setDirty(false);
    }, [config, flag.variations]);

    useEffect(() => {
        segmentsApi.list(projectId).then(setSegments).catch(() => setSegments([]));
    }, [projectId]);

    useEffect(() => {
        approvalsApi
            .list(projectId, "pending")
            .then((requests) => {
                const match = requests.find(
                    (request) => request.flagId === flag._id && request.environmentKey === environment.key,
                );
                setPendingApproval(match || null);
            })
            .catch(() => setPendingApproval(null));
    }, [projectId, flag._id, environment.key, dirty === false ? config : null]);

    const markDirty = (updater) => (...args) => {
        updater(...args);
        setDirty(true);
    };

    const setEnabledDirty = markDirty(setEnabled);
    const setOffVariationIdDirty = markDirty(setOffVariationId);
    const setTargetRowsDirty = markDirty(setTargetRows);
    const setRulesDirty = markDirty(setRules);
    const setFallthroughDirty = markDirty(setFallthrough);

    const segmentHint = useMemo(() => segments.map((segment) => segment.key).join(", "), [segments]);

    const save = async () => {
        setSaving(true);
        try {
            const targets = Array.from(targetRows.entries())
                .map(([variationId, text]) => ({
                    variationId,
                    userKeys: text
                        .split(",")
                        .map((key) => key.trim())
                        .filter(Boolean),
                }))
                .filter((target) => target.userKeys.length > 0);

            const payload = {
                enabled,
                offVariationId,
                targets,
                rules: rules.map((rule) => ({
                    id: rule.id,
                    description: rule.description,
                    clauses: rule.clauses,
                    variationId: rule.variationId || null,
                    rollout: rule.rollout || [],
                })),
                fallthroughVariationId: fallthrough.variationId || null,
                fallthroughRollout: fallthrough.rollout || [],
            };

            const result = await flagsApi.submitChange(flag._id, environment.key, payload);
            if (result.applied) {
                show("Configuration updated.", { tone: "success" });
            } else {
                show(`Submitted for approval - ${environment.name} requires review before this goes live.`, {
                    tone: "info",
                });
            }
            setDirty(false);
            onSaved?.();
        } catch (caught) {
            show(caught.message || "Unable to save this configuration.", { tone: "error" });
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="card-list">
            {pendingApproval && (
                <div className="card" style={{ borderColor: "var(--warning)" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <MaterialIcon size={20}>approval</MaterialIcon>
                        <span>
                            A change proposed by <strong>{pendingApproval.requestedByName}</strong> is awaiting approval
                            for {environment.name}.
                        </span>
                    </div>
                </div>
            )}

            <EvaluatorPanel flag={flag} projectId={projectId} environmentKey={environment.key} />

            <div className="card">
                <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                    <Switch ariaLabel={`Enable in ${environment.name}`} checked={enabled} onChange={setEnabledDirty} />
                    <div>
                        <strong>{enabled ? "On" : "Off"} in {environment.name}</strong>
                        <p style={{ margin: "2px 0 0", fontSize: 12.5, color: "var(--muted)" }}>
                            {enabled
                                ? "Serving variations based on targets, rules, and the default below."
                                : "Everyone receives the off variation until this is turned on."}
                        </p>
                    </div>
                </div>
                {!enabled && (
                    <div style={{ marginTop: 14, maxWidth: 260 }}>
                        <SelectMenu
                            ariaLabel="Off variation"
                            value={offVariationId}
                            onChange={setOffVariationIdDirty}
                            options={flag.variations.map((variation) => ({ value: variation.id, label: variation.name }))}
                        />
                    </div>
                )}
            </div>

            <div className="card">
                <h3>Individual targeting</h3>
                <p className="card-subtitle">
                    Always serve a specific variation to named users, regardless of rules below.
                </p>
                {flag.variations.map((variation) => (
                    <div className="field-group" key={variation.id}>
                        <label htmlFor={`target-${variation.id}`}>{variation.name}</label>
                        <input
                            id={`target-${variation.id}`}
                            placeholder="user keys, comma separated"
                            value={targetRows.get(variation.id) || ""}
                            onChange={(event) =>
                                setTargetRowsDirty((current) => new Map(current).set(variation.id, event.target.value))
                            }
                        />
                    </div>
                ))}
            </div>

            <div className="card">
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <h3>Targeting rules</h3>
                    <button
                        className="link-button"
                        type="button"
                        onClick={() => setRulesDirty((current) => [...current, newRule(flag.variations)])}
                    >
                        + Add rule
                    </button>
                </div>
                {segments.length > 0 && (
                    <p style={{ fontSize: 12, color: "var(--faint)" }}>Available segments: {segmentHint}</p>
                )}
                {rules.length === 0 && <p style={{ color: "var(--muted)", fontSize: 13 }}>No rules yet - everyone falls through to the default below.</p>}
                {rules.map((rule, index) => (
                    <RuleEditor
                        index={index}
                        key={rule.id}
                        rule={rule}
                        variations={flag.variations}
                        onChange={(next) => setRulesDirty((current) => current.map((candidate, i) => (i === index ? next : candidate)))}
                        onRemove={() => setRulesDirty((current) => current.filter((_, i) => i !== index))}
                    />
                ))}
            </div>

            <div className="card">
                <h3>Default rule (fallthrough)</h3>
                <p className="card-subtitle">
                    Applies when no individual target or rule above matches.
                </p>
                <OutcomeEditor outcome={fallthrough} variations={flag.variations} onChange={setFallthroughDirty} />
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, alignItems: "center" }}>
                {environment.production && <Badge tone="danger">Requires approval in Production</Badge>}
                <button className="primary-button" disabled={!dirty || saving} type="button" onClick={save}>
                    {saving ? "Saving…" : "Save changes"}
                </button>
            </div>
        </div>
    );
}
