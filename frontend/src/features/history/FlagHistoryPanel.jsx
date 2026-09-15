import { useCallback, useEffect, useState } from "react";
import { historyApi } from "./history.api.js";
import { flagsApi } from "../flags/flags.api.js";
import { useToast } from "../../shared/components/Toast.jsx";
import { ConfirmationDialog } from "../../shared/components/ConfirmationDialog.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { EmptyState } from "../../shared/components/EmptyState.jsx";
import { Spinner } from "../../shared/components/Spinner.jsx";

const ACTION_META = {
    FLAG_CREATED: {
        icon: "add",
        label: "Flag created",
        background: "var(--success-container)",
        color: "var(--success-ink)",
    },
    FLAG_UPDATED: {
        icon: "edit",
        label: "Flag updated",
        background: "var(--accent-tint)",
        color: "var(--accent-ink)",
    },
    FLAG_ARCHIVED: {
        icon: "delete",
        label: "Flag archived",
        background: "var(--danger-container)",
        color: "var(--danger-ink)",
    },
    FLAG_RESTORED: {
        icon: "history",
        label: "Flag restored",
        background: "var(--success-container)",
        color: "var(--success-ink)",
    },
    CONFIG_UPDATED: {
        icon: "edit",
        label: "Configuration updated",
        background: "var(--accent-tint)",
        color: "var(--accent-ink)",
    },
    CHANGE_PROPOSED: {
        icon: "approval",
        label: "Change proposed",
        background: "var(--accent-tint)",
        color: "var(--accent-ink)",
    },
    CHANGE_SCHEDULED: {
        icon: "schedule",
        label: "Change scheduled",
        background: "var(--warning-container)",
        color: "var(--warning-ink)",
    },
    CHANGE_REJECTED: {
        icon: "close",
        label: "Change rejected",
        background: "var(--danger-container)",
        color: "var(--danger-ink)",
    },
    CHANGE_CANCELLED: {
        icon: "close",
        label: "Change cancelled",
        background: "var(--field)",
        color: "var(--muted)",
    },
    ROLLED_BACK: {
        icon: "history",
        label: "Rolled back",
        background: "var(--warning-container)",
        color: "var(--warning-ink)",
    },
    DEFAULT: {
        icon: "edit",
        label: "Updated",
        background: "var(--accent-tint)",
        color: "var(--accent-ink)",
    },
};

function getActionMeta(action) {
    return ACTION_META[action] || ACTION_META.DEFAULT;
}

function formatDateTime(value) {
    return value ? new Date(value).toLocaleString() : "Unknown time";
}

export function FlagHistoryPanel({ flagId, environment, onRestored }) {
    const { show } = useToast();
    const environmentKey = environment?.key;
    const [entries, setEntries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [restoreTarget, setRestoreTarget] = useState(null);
    const [restoreBusy, setRestoreBusy] = useState(false);
    const [restoreError, setRestoreError] = useState("");

    const load = useCallback(async () => {
        if (!flagId) {
            setEntries([]);
            setLoading(false);
            setError("");
            return;
        }

        setLoading(true);
        setError("");
        try {
            const data = await historyApi.listForFlag(flagId);
            setEntries(Array.isArray(data) ? data : []);
        } catch (caught) {
            setError(caught.message || "Unable to load flag history.");
        } finally {
            setLoading(false);
        }
    }, [flagId]);

    useEffect(() => {
        load();
    }, [load]);

    const closeRestoreDialog = () => {
        if (restoreBusy) return;
        setRestoreTarget(null);
        setRestoreError("");
    };

    const confirmRestore = async () => {
        if (!restoreTarget || !environmentKey) return;

        setRestoreBusy(true);
        setRestoreError("");
        try {
            const result = await flagsApi.restoreConfig(flagId, environmentKey, restoreTarget._id);
            if (result.applied) {
                show(`Restored ${environmentKey} configuration from ${formatDateTime(restoreTarget.createdAt)}.`, {
                    tone: "success",
                });
            } else {
                show(`Submitted for approval - ${environment.name} requires review before this rollback goes live.`, {
                    tone: "info",
                });
            }
            setRestoreTarget(null);
            await load();
            onRestored?.();
        } catch (caught) {
            const message = caught.message || "Unable to restore this version.";
            setRestoreError(message);
            show(message, { tone: "error" });
        } finally {
            setRestoreBusy(false);
        }
    };

    return (
        <section className="card">
            <div
                style={{
                    display: "flex",
                    alignItems: "flex-start",
                    justifyContent: "space-between",
                    gap: 12,
                    marginBottom: 16,
                    flexWrap: "wrap",
                }}
            >
                <div>
                    <h3 style={{ margin: 0, fontSize: 16 }}>History</h3>
                    <p style={{ margin: "6px 0 0", color: "var(--muted)", fontSize: 13.5 }}>
                        Review recent changes for this flag and restore an earlier {environmentKey || "environment"} configuration.
                    </p>
                </div>
                <button className="link-button" disabled={loading} type="button" onClick={load}>
                    {loading ? "Refreshing…" : "Refresh"}
                </button>
            </div>

            {loading && <Spinner label="Loading flag history" />}
            {!loading && error && (
                <p className="inline-error" role="alert">
                    {error}
                </p>
            )}
            {!loading && !error && entries.length === 0 && (
                <EmptyState
                    icon="history"
                    title="No history for this flag yet"
                    description="Changes, proposals, approvals, and rollbacks for this flag will appear here."
                />
            )}
            {!loading && !error && entries.length > 0 && (
                <div className="timeline">
                    {entries.map((entry) => {
                        const actionMeta = getActionMeta(entry.action);
                        const canRestore = entry.environmentKey === environmentKey && Boolean(entry.afterSnapshot);
                        const isCurrentEnvironment = entry.environmentKey === environmentKey || entry.environmentKey == null;

                        return (
                            <div className="timeline-entry" key={entry._id}>
                                <div
                                    className="timeline-dot"
                                    style={{ background: actionMeta.background, color: actionMeta.color }}
                                >
                                    <MaterialIcon size={16}>{actionMeta.icon}</MaterialIcon>
                                </div>
                                <div
                                    className="timeline-body"
                                    style={isCurrentEnvironment ? undefined : { opacity: 0.8 }}
                                >
                                    <div className="timeline-header">
                                        <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                                            <strong>{actionMeta.label}</strong>
                                            {entry.environmentKey && <Badge tone="info">{entry.environmentKey}</Badge>}
                                        </div>
                                        <span className="timeline-time">{formatDateTime(entry.createdAt)}</span>
                                    </div>
                                    <p className="timeline-summary">{entry.summary || actionMeta.label}</p>
                                    <div
                                        style={{
                                            marginTop: 8,
                                            display: "flex",
                                            alignItems: "center",
                                            justifyContent: "space-between",
                                            gap: 12,
                                            flexWrap: "wrap",
                                        }}
                                    >
                                        <span style={{ color: "var(--muted)", fontSize: 12.5 }}>
                                            by {entry.actorName || "Unknown actor"}
                                        </span>
                                        {canRestore && (
                                            <button
                                                className="link-button"
                                                disabled={restoreBusy}
                                                type="button"
                                                onClick={() => {
                                                    setRestoreTarget(entry);
                                                    setRestoreError("");
                                                }}
                                            >
                                                Restore this version
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {restoreTarget && (
                <ConfirmationDialog
                    busy={restoreBusy}
                    confirmLabel={environment?.production ? "Submit for approval" : "Restore version"}
                    error={restoreError}
                    message={
                        environment?.production
                            ? `This will submit the ${formatDateTime(restoreTarget.createdAt)} configuration for approval. It won't take effect in ${environmentKey} until a teammate reviews it.`
                            : `This will replace the current ${environmentKey} configuration with the one from ${formatDateTime(restoreTarget.createdAt)}.`
                    }
                    title={`Restore ${environmentKey} configuration?`}
                    onCancel={closeRestoreDialog}
                    onConfirm={confirmRestore}
                />
            )}
        </section>
    );
}
