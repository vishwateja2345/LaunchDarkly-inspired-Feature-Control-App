import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { experimentsApi } from "./experiments.api.js";
import { useToast } from "../../shared/components/Toast.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { EmptyState } from "../../shared/components/EmptyState.jsx";
import { Spinner } from "../../shared/components/Spinner.jsx";

const NUMBER_FORMAT = new Intl.NumberFormat("en-US");
const PERCENT_FORMAT = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 1,
    maximumFractionDigits: 1,
});

function clampPercent(value) {
    const numericValue = Number(value) || 0;
    return Math.max(0, Math.min(100, numericValue));
}

function formatCount(value) {
    return NUMBER_FORMAT.format(Number(value) || 0);
}

function formatPercent(value) {
    return `${PERCENT_FORMAT.format(Number(value) || 0)}%`;
}

function formatLift(value) {
    if (value == null || Number.isNaN(Number(value))) return "—";
    const numericValue = Number(value);
    return `${numericValue > 0 ? "+" : ""}${PERCENT_FORMAT.format(numericValue)}%`;
}

function getLiftTone(value) {
    if (value == null) return "neutral";
    if (Number(value) > 0) return "success";
    if (Number(value) < 0) return "danger";
    return "neutral";
}

function buildComparisonRows(flagVariations, comparison) {
    const variationList = Array.isArray(flagVariations) ? flagVariations : [];
    const comparisonRows = Array.isArray(comparison?.variations) ? comparison.variations : [];
    const comparisonById = new Map(comparisonRows.map((row) => [row.variationId, row]));
    const flagById = new Map(variationList.map((variation) => [variation.id, variation]));
    const controlVariationId = comparison?.controlVariationId ?? null;
    const orderedIds = variationList.map((variation) => variation.id);

    if (controlVariationId && orderedIds.includes(controlVariationId)) {
        orderedIds.splice(orderedIds.indexOf(controlVariationId), 1);
        orderedIds.unshift(controlVariationId);
    }

    const knownRows = orderedIds.map((variationId, index) => {
        const variation = flagById.get(variationId);
        const row = comparisonById.get(variationId);
        const isControl = variationId === controlVariationId;

        return {
            variationId,
            variationName:
                row?.variationName ||
                variation?.name ||
                variation?.value ||
                `Variation ${index + 1}`,
            exposures: Number(row?.exposures) || 0,
            conversions: Number(row?.conversions) || 0,
            conversionRate: Number(row?.conversionRate) || 0,
            relativeLiftPercent: isControl ? null : row?.relativeLiftPercent ?? null,
            isSignificant: isControl ? null : row?.isSignificant ?? null,
            isControl,
        };
    });

    const extraRows = comparisonRows
        .filter((row) => !flagById.has(row.variationId))
        .map((row, index) => ({
            variationId: row.variationId,
            variationName: row.variationName || `Variation ${knownRows.length + index + 1}`,
            exposures: Number(row.exposures) || 0,
            conversions: Number(row.conversions) || 0,
            conversionRate: Number(row.conversionRate) || 0,
            relativeLiftPercent: row.variationId === controlVariationId ? null : row.relativeLiftPercent ?? null,
            isSignificant: row.variationId === controlVariationId ? null : row.isSignificant ?? null,
            isControl: row.variationId === controlVariationId,
        }));

    return [...knownRows, ...extraRows];
}

export function ExperimentPanel({ flag, environmentKey }) {
    const { show } = useToast();
    const metricRequestRef = useRef(0);
    const comparisonRequestRef = useRef(0);
    const flagVariations = Array.isArray(flag?.variations) ? flag.variations : [];
    const scopeKey = `${flag?._id || "unknown"}:${environmentKey || "unknown"}`;
    const [metricKeys, setMetricKeys] = useState([]);
    const [metricSelection, setMetricSelection] = useState({
        scopeKey: "",
        value: "",
    });
    const [comparison, setComparison] = useState(null);
    const [loadingMetrics, setLoadingMetrics] = useState(false);
    const [loadingComparison, setLoadingComparison] = useState(false);
    const [comparisonError, setComparisonError] = useState("");
    const [simulating, setSimulating] = useState(false);
    const currentMetric = metricSelection.scopeKey === scopeKey ? metricSelection.value : "";

    const rows = useMemo(
        () => buildComparisonRows(flagVariations, comparison),
        [comparison, flagVariations],
    );

    const winnerVariationId = useMemo(() => {
        const activeRows = rows.filter((row) => row.exposures > 0);
        if (!activeRows.length) return null;
        return activeRows.reduce((best, row) => {
            if (!best || row.conversionRate > best.conversionRate) return row;
            return best;
        }, null)?.variationId ?? null;
    }, [rows]);

    const metricOptions = useMemo(
        () => metricKeys.map((metricKey) => ({ value: metricKey, label: metricKey })),
        [metricKeys],
    );

    const loadComparison = useCallback(
        async (metricValue) => {
            if (!flag?._id || !environmentKey) {
                setComparison(null);
                setComparisonError("");
                return null;
            }

            const requestId = ++comparisonRequestRef.current;
            setLoadingComparison(true);
            setComparisonError("");

            try {
                const result = await experimentsApi.compare(
                    flag._id,
                    environmentKey,
                    metricValue?.trim() ? metricValue.trim() : undefined,
                );
                if (requestId !== comparisonRequestRef.current) return null;
                setComparison(result);
                return result;
            } catch (caught) {
                if (requestId !== comparisonRequestRef.current) return null;
                setComparison(null);
                const message = caught.message || "Unable to load experiment comparison.";
                setComparisonError(message);
                show(message, { tone: "error" });
                return null;
            } finally {
                if (requestId === comparisonRequestRef.current) setLoadingComparison(false);
            }
        },
        [environmentKey, flag?._id, show],
    );

    useEffect(() => {
        if (!flag?._id || !environmentKey) return undefined;

        const requestId = ++metricRequestRef.current;
        setLoadingMetrics(true);
        setMetricKeys([]);
        setMetricSelection({ scopeKey, value: "" });

        let active = true;

        (async () => {
            try {
                const result = await experimentsApi.metricKeys(flag._id, environmentKey);
                if (!active || requestId !== metricRequestRef.current) return;
                const nextMetricKeys = Array.isArray(result) ? result.filter(Boolean) : [];
                setMetricKeys(nextMetricKeys);
                setMetricSelection({
                    scopeKey,
                    value: nextMetricKeys[0] || "",
                });
            } catch (caught) {
                if (!active || requestId !== metricRequestRef.current) return;
                setMetricKeys([]);
                setMetricSelection({ scopeKey, value: "" });
                show(caught.message || "Unable to load experiment metrics.", { tone: "error" });
            } finally {
                if (active && requestId === metricRequestRef.current) setLoadingMetrics(false);
            }
        })();

        return () => {
            active = false;
        };
    }, [environmentKey, flag?._id, scopeKey, show]);

    useEffect(() => {
        loadComparison(currentMetric);
    }, [currentMetric, loadComparison]);

    const retryComparison = useCallback(() => {
        loadComparison(currentMetric);
    }, [currentMetric, loadComparison]);

    const simulateTraffic = useCallback(async () => {
        if (!flag?._id || !environmentKey || !flagVariations.length || simulating) return;

        const metricKeyForSimulation = currentMetric.trim() || "demo_metric";
        const visitsPerVariation = Math.max(1, Math.floor(12 / flagVariations.length));
        let totalVisits = 0;

        setSimulating(true);

        try {
            const tasks = [];

            for (let round = 0; round < visitsPerVariation; round += 1) {
                for (let index = 0; index < flagVariations.length; index += 1) {
                    const variation = flagVariations[index];
                    const userKey = `demo-${Date.now()}-${round}-${index}-${Math.random().toString(36).slice(2, 8)}`;
                    totalVisits += 1;

                    tasks.push((async () => {
                        await experimentsApi.recordExposure(flag._id, environmentKey, {
                            userKey,
                            variationId: variation.id,
                        });

                        if (Math.random() < 0.3) {
                            await experimentsApi.recordConversion(flag._id, environmentKey, {
                                userKey,
                                metricKey: metricKeyForSimulation,
                                value: 10,
                            });
                        }
                    })());
                }
            }

            await Promise.all(tasks);

            if (!currentMetric.trim()) {
                setMetricSelection({
                    scopeKey,
                    value: metricKeyForSimulation,
                });
                setMetricKeys((existingKeys) => (
                    existingKeys.includes(metricKeyForSimulation)
                        ? existingKeys
                        : [metricKeyForSimulation, ...existingKeys]
                ));
            }

            await loadComparison(metricKeyForSimulation);
            show(`Simulated ${totalVisits} visits`, { tone: "success" });
        } catch (caught) {
            show(caught.message || "Unable to simulate experiment traffic.", { tone: "error" });
        } finally {
            setSimulating(false);
        }
    }, [
        currentMetric,
        environmentKey,
        flag?._id,
        flagVariations,
        loadComparison,
        scopeKey,
        show,
        simulating,
    ]);

    if (!flagVariations.length) {
        return (
            <section className="card" aria-labelledby="experiment-panel-title">
                <h2 id="experiment-panel-title" style={{ marginTop: 0 }}>
                    Experiment results
                </h2>
                <EmptyState
                    description="Add at least one variation to this flag before comparing experiment performance."
                    icon="experiment"
                    title="No variations available"
                />
            </section>
        );
    }

    return (
        <section className="card" aria-labelledby="experiment-panel-title">
            <div
                style={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    gap: 12,
                    flexWrap: "wrap",
                    marginBottom: 16,
                }}
            >
                <div>
                    <h2 id="experiment-panel-title" style={{ margin: 0 }}>
                        Experiment results
                    </h2>
                    <p style={{ margin: "6px 0 0", color: "var(--muted)", fontSize: 13 }}>
                        Compare exposures, conversion rate, lift, and significance by variation.
                    </p>
                </div>
                {(loadingMetrics || loadingComparison) && <Spinner label="Loading experiment data" />}
            </div>

            <div className="page-toolbar" role="group" aria-label="Experiment metric controls">
                <div className="field-group" style={{ marginBottom: 0, minWidth: 220 }}>
                    <label>Metric</label>
                    {metricOptions.length ? (
                        <SelectMenu
                            ariaLabel="Select experiment metric"
                            disabled={loadingMetrics || simulating}
                            options={metricOptions}
                            value={currentMetric || metricOptions[0]?.value}
                            onChange={(value) => setMetricSelection({ scopeKey, value })}
                        />
                    ) : (
                        <input
                            id="experiment-metric-input"
                            disabled={loadingMetrics || simulating}
                            placeholder="checkout_completed"
                            type="text"
                            value={currentMetric}
                            onChange={(event) => {
                                setMetricSelection({
                                    scopeKey,
                                    value: event.target.value,
                                });
                            }}
                        />
                    )}
                </div>

                <button
                    className="secondary-button"
                    disabled={simulating || loadingComparison || !flagVariations.length}
                    type="button"
                    onClick={simulateTraffic}
                >
                    {simulating ? "Simulating…" : "Simulate traffic"}
                </button>
            </div>

            {!metricOptions.length && (
                <p style={{ margin: "0 0 16px", color: "var(--muted)", fontSize: 13 }}>
                    No recorded metric keys yet. Enter one to inspect conversions or use the demo traffic button.
                </p>
            )}

            {comparisonError ? (
                <EmptyState
                    action={(
                        <button className="secondary-button" type="button" onClick={retryComparison}>
                            Retry
                        </button>
                    )}
                    description={comparisonError}
                    icon="experiment"
                    title="Unable to load experiment results"
                />
            ) : loadingComparison && !comparison ? (
                <Spinner label="Loading experiment comparison" />
            ) : (
                <div style={{ overflowX: "auto" }}>
                    <table className="experiment-table">
                        <thead>
                            <tr>
                                <th scope="col">Variation</th>
                                <th scope="col">Exposures</th>
                                <th scope="col">Conversions</th>
                                <th scope="col">Conversion rate</th>
                                <th scope="col">Relative lift</th>
                                <th scope="col">Significant</th>
                            </tr>
                        </thead>
                        <tbody>
                            {rows.map((row) => (
                                <tr
                                    className={row.variationId === winnerVariationId ? "experiment-winner-row" : undefined}
                                    key={row.variationId}
                                >
                                    <td>
                                        <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                                            <strong>{row.variationName}</strong>
                                            {row.isControl && <Badge tone="neutral">Control</Badge>}
                                        </div>
                                    </td>
                                    <td>{formatCount(row.exposures)}</td>
                                    <td>{formatCount(row.conversions)}</td>
                                    <td style={{ minWidth: 190 }}>
                                        <div>{formatPercent(row.conversionRate)}</div>
                                        <div
                                            className="experiment-bar-track"
                                            aria-hidden="true"
                                        >
                                            <div
                                                className="experiment-bar-fill"
                                                style={{ width: `${clampPercent(row.conversionRate)}%` }}
                                            />
                                        </div>
                                    </td>
                                    <td>
                                        {row.relativeLiftPercent == null ? (
                                            "—"
                                        ) : (
                                            <Badge tone={getLiftTone(row.relativeLiftPercent)}>
                                                {formatLift(row.relativeLiftPercent)}
                                            </Badge>
                                        )}
                                    </td>
                                    <td>
                                        {row.isSignificant == null ? (
                                            "—"
                                        ) : row.isSignificant ? (
                                            <Badge tone="success">Significant</Badge>
                                        ) : (
                                            <Badge tone="neutral">Not yet significant</Badge>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
}
