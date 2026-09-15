import { useCallback, useEffect, useState } from "react";
import { historyApi } from "./history.api.js";
import { useWorkspace } from "../../shared/WorkspaceContext.jsx";
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

export function ProjectHistoryPage() {
    const { project } = useWorkspace();
    const [entries, setEntries] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const load = useCallback(async () => {
        if (!project) {
            setEntries([]);
            setLoading(false);
            setError("");
            return;
        }

        setLoading(true);
        setError("");
        try {
            const data = await historyApi.listForProject(project._id, 100);
            setEntries(Array.isArray(data) ? data : []);
        } catch (caught) {
            setError(caught.message || "Unable to load project history.");
        } finally {
            setLoading(false);
        }
    }, [project]);

    useEffect(() => {
        load();
    }, [load]);

    if (!project) return null;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Change history</h2>
                    <p>Review the latest flag and configuration changes across {project.name}.</p>
                </div>
                <button className="secondary-button" disabled={loading} type="button" onClick={load}>
                    {loading ? "Refreshing…" : "Refresh"}
                </button>
            </div>

            {loading && <Spinner label="Loading project history" />}
            {!loading && error && (
                <section className="card">
                    <p className="inline-error" role="alert">
                        {error}
                    </p>
                </section>
            )}
            {!loading && !error && entries.length === 0 && (
                <section className="card">
                    <EmptyState
                        icon="history"
                        title="No change history yet"
                        description="Project-wide activity will appear here once teammates start creating and updating flags."
                    />
                </section>
            )}
            {!loading && !error && entries.length > 0 && (
                <section className="card">
                    <div className="timeline">
                        {entries.map((entry) => {
                            const actionMeta = getActionMeta(entry.action);
                            return (
                                <div className="timeline-entry" key={entry._id}>
                                    <div
                                        className="timeline-dot"
                                        style={{ background: actionMeta.background, color: actionMeta.color }}
                                    >
                                        <MaterialIcon size={16}>{actionMeta.icon}</MaterialIcon>
                                    </div>
                                    <div className="timeline-body">
                                        <div className="timeline-header">
                                            <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                                                <strong>{actionMeta.label}</strong>
                                                {entry.environmentKey && <Badge tone="info">{entry.environmentKey}</Badge>}
                                            </div>
                                            <span className="timeline-time">{formatDateTime(entry.createdAt)}</span>
                                        </div>
                                        <p className="timeline-summary">{entry.summary || actionMeta.label}</p>
                                        <p style={{ margin: "8px 0 0", color: "var(--muted)", fontSize: 12.5 }}>
                                            by {entry.actorName || "Unknown actor"}
                                        </p>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </section>
            )}
        </div>
    );
}
