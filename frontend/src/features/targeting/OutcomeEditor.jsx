import { useEffect } from "react";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";

const BAR_COLORS = ["#6366f1", "#38bdf8", "#f59e0b", "#f472b6", "#4ade80", "#a78bfa", "#fb923c"];

function evenSplit(variations) {
    const count = variations.length || 1;
    const base = Math.floor((100 / count) * 10) / 10;
    const weights = variations.map(() => base);
    const remainder = Math.round((100 - base * count) * 10) / 10;
    weights[weights.length - 1] = Math.round((weights[weights.length - 1] + remainder) * 10) / 10;
    return variations.map((variation, index) => ({ variationId: variation.id, weight: weights[index] }));
}

/**
 * Shared outcome editor used both by a targeting rule's result and by the flag's
 * fallthrough (default) result: either serve one fixed variation, or split traffic
 * across variations by percentage (a progressive rollout).
 */
export function OutcomeEditor({ onChange, outcome, variations }) {
    const mode = outcome.rollout && outcome.rollout.length > 0 ? "rollout" : "variation";
    const variationIsValid = variations.some((variation) => variation.id === outcome.variationId);

    // The variation dropdown visually falls back to the first option when nothing is
    // selected yet, so without this the underlying value could stay null/stale while the
    // UI looks fully filled in - self-heal immediately so what's shown always matches state.
    useEffect(() => {
        if (mode === "variation" && !variationIsValid && variations[0]) {
            onChange({ variationId: variations[0].id, rollout: [] });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [mode, variationIsValid, variations[0]?.id]);

    const setMode = (nextMode) => {
        if (nextMode === "rollout") {
            onChange({ variationId: null, rollout: outcome.rollout?.length ? outcome.rollout : evenSplit(variations) });
        } else {
            onChange({ variationId: outcome.variationId || variations[0]?.id, rollout: [] });
        }
    };

    const updateWeight = (variationId, weight) => {
        const numeric = Math.max(0, Math.min(100, Number(weight) || 0));
        onChange({
            variationId: null,
            rollout: outcome.rollout.map((entry) => (entry.variationId === variationId ? { ...entry, weight: numeric } : entry)),
        });
    };

    const total = (outcome.rollout || []).reduce((sum, entry) => sum + entry.weight, 0);
    const totalRounded = Math.round(total * 10) / 10;

    return (
        <div className="rule-outcome">
            <div className="field-row" style={{ marginBottom: 10 }}>
                <label className="checkbox-row">
                    <input type="radio" checked={mode === "variation"} onChange={() => setMode("variation")} />
                    Serve one variation
                </label>
                <label className="checkbox-row">
                    <input type="radio" checked={mode === "rollout"} onChange={() => setMode("rollout")} />
                    Split by percentage (rollout)
                </label>
            </div>

            {mode === "variation" && (
                <SelectMenu
                    ariaLabel="Variation to serve"
                    value={outcome.variationId}
                    onChange={(variationId) => onChange({ variationId, rollout: [] })}
                    options={variations.map((variation) => ({ value: variation.id, label: variation.name }))}
                />
            )}

            {mode === "rollout" && (
                <div className="rollout-editor">
                    <div className="rollout-bar" aria-hidden="true">
                        {(outcome.rollout || []).map((entry, index) => (
                            <div
                                className="rollout-bar-segment"
                                key={entry.variationId}
                                style={{ width: `${entry.weight}%`, background: BAR_COLORS[index % BAR_COLORS.length] }}
                            />
                        ))}
                    </div>
                    {(outcome.rollout || []).map((entry, index) => {
                        const variation = variations.find((candidate) => candidate.id === entry.variationId);
                        return (
                            <div className="rollout-row" key={entry.variationId}>
                                <span
                                    className="rollout-row-label"
                                    style={{ color: BAR_COLORS[index % BAR_COLORS.length] }}
                                >
                                    {variation?.name || entry.variationId}
                                </span>
                                <input
                                    aria-label={`${variation?.name || entry.variationId} percentage (slider)`}
                                    max={100}
                                    min={0}
                                    step={1}
                                    type="range"
                                    value={entry.weight}
                                    onChange={(event) => updateWeight(entry.variationId, event.target.value)}
                                />
                                <input
                                    aria-label={`${variation?.name || entry.variationId} percentage`}
                                    max={100}
                                    min={0}
                                    step={0.1}
                                    type="number"
                                    value={entry.weight}
                                    onChange={(event) => updateWeight(entry.variationId, event.target.value)}
                                />
                                <span>%</span>
                            </div>
                        );
                    })}
                    <span className={totalRounded === 100 ? "rollout-sum-ok" : "rollout-sum-warning"}>
                        {totalRounded === 100 ? "✓ Adds up to 100%" : `Currently ${totalRounded}% - must add up to 100%`}
                    </span>
                </div>
            )}
        </div>
    );
}
