import { useEffect, useState } from "react";
import { useWorkspace } from "../../shared/WorkspaceContext.jsx";
import { useRouter } from "../../shared/utils/router.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { Spinner } from "../../shared/components/Spinner.jsx";
import { flagsApi } from "../flags/flags.api.js";
import { segmentsApi } from "../targeting/targeting.api.js";
import { approvalsApi } from "../approvals/approvals.api.js";
import { historyApi } from "../history/history.api.js";

const ACTION_ICON = {
    FLAG_CREATED: "add",
    FLAG_UPDATED: "edit",
    CONFIG_UPDATED: "edit",
    FLAG_ARCHIVED: "delete",
    FLAG_RESTORED: "history",
    ROLLED_BACK: "history",
    CHANGE_PROPOSED: "approval",
    CHANGE_SCHEDULED: "schedule",
    CHANGE_REJECTED: "close",
    CHANGE_CANCELLED: "close",
};

export function DashboardPage() {
    const { project } = useWorkspace();
    const { navigate } = useRouter();
    const [stats, setStats] = useState(null);
    const [recent, setRecent] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!project) return;
        let cancelled = false;
        setLoading(true);
        setError("");

        Promise.all([
            flagsApi.list(project._id, { includeArchived: true }),
            segmentsApi.list(project._id),
            approvalsApi.list(project._id, "pending"),
            historyApi.listForProject(project._id, 8),
        ])
            .then(([flags, segments, pending, history]) => {
                if (cancelled) return;
                const active = flags.filter((flag) => !flag.archived);
                const temporary = active.filter((flag) => flag.temporary);
                setStats({
                    activeFlags: active.length,
                    archivedFlags: flags.length - active.length,
                    temporaryFlags: temporary.length,
                    segments: segments.length,
                    pendingApprovals: pending.length,
                    pendingRequests: pending,
                });
                setRecent(history);
            })
            .catch((caught) => !cancelled && setError(caught.message || "Unable to load the dashboard."))
            .finally(() => !cancelled && setLoading(false));

        return () => {
            cancelled = true;
        };
    }, [project]);

    if (!project) return null;
    if (loading) return <Spinner label="Loading dashboard" />;
    if (error) return <p className="inline-error">{error}</p>;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Welcome back</h2>
                    <p>Here is what is happening in {project.name} right now.</p>
                </div>
                <button className="primary-button" type="button" onClick={() => navigate(`/p/${project.key}/flags`)}>
                    <MaterialIcon size={18}>flag</MaterialIcon> Go to flags
                </button>
            </div>

            <div className="dashboard-stats">
                <button className="dashboard-stat-card" type="button" onClick={() => navigate(`/p/${project.key}/flags`)}>
                    <MaterialIcon size={22}>flag</MaterialIcon>
                    <strong>{stats.activeFlags}</strong>
                    <span>Active flags</span>
                </button>
                <button className="dashboard-stat-card" type="button" onClick={() => navigate(`/p/${project.key}/flags`)}>
                    <MaterialIcon size={22}>bolt</MaterialIcon>
                    <strong>{stats.temporaryFlags}</strong>
                    <span>Temporary / rollout flags</span>
                </button>
                <button className="dashboard-stat-card" type="button" onClick={() => navigate(`/p/${project.key}/segments`)}>
                    <MaterialIcon size={22}>group</MaterialIcon>
                    <strong>{stats.segments}</strong>
                    <span>Audience segments</span>
                </button>
                <button
                    className={stats.pendingApprovals > 0 ? "dashboard-stat-card dashboard-stat-card-alert" : "dashboard-stat-card"}
                    type="button"
                    onClick={() => navigate(`/p/${project.key}/approvals`)}
                >
                    <MaterialIcon size={22}>approval</MaterialIcon>
                    <strong>{stats.pendingApprovals}</strong>
                    <span>Pending approvals</span>
                </button>
            </div>

            <div className="dashboard-grid">
                <section className="card">
                    <h3>Recent activity</h3>
                    {recent.length === 0 && <p style={{ color: "var(--muted)", fontSize: 13.5 }}>No changes recorded yet.</p>}
                    <div className="timeline">
                        {recent.map((entry) => (
                            <div className="timeline-entry" key={entry._id}>
                                <div className="timeline-dot">
                                    <MaterialIcon size={16}>{ACTION_ICON[entry.action] || "edit"}</MaterialIcon>
                                </div>
                                <div className="timeline-body">
                                    <div className="timeline-header">
                                        <span>{entry.actorName}</span>
                                        <span className="timeline-time">{new Date(entry.createdAt).toLocaleString()}</span>
                                    </div>
                                    <p className="timeline-summary">{entry.summary}</p>
                                    {entry.environmentKey && <Badge tone="neutral">{entry.environmentKey}</Badge>}
                                </div>
                            </div>
                        ))}
                    </div>
                    <button className="link-button" type="button" onClick={() => navigate(`/p/${project.key}/history`)}>
                        View full history
                    </button>
                </section>

                <section className="card">
                    <h3>Awaiting your review</h3>
                    {stats.pendingRequests.length === 0 && (
                        <p style={{ color: "var(--muted)", fontSize: 13.5 }}>Nothing pending - production changes appear here.</p>
                    )}
                    <div className="card-list">
                        {stats.pendingRequests.slice(0, 5).map((request) => (
                            <button
                                className="dashboard-approval-row"
                                key={request._id}
                                type="button"
                                onClick={() => navigate(`/p/${project.key}/approvals`)}
                            >
                                <div>
                                    <strong>{request.flagName}</strong>
                                    <span> in {request.environmentName}</span>
                                </div>
                                <span style={{ color: "var(--muted)", fontSize: 12.5 }}>by {request.requestedByName}</span>
                            </button>
                        ))}
                    </div>
                    {stats.pendingApprovals > 0 && (
                        <button className="link-button" type="button" onClick={() => navigate(`/p/${project.key}/approvals`)}>
                            Review all {stats.pendingApprovals}
                        </button>
                    )}
                </section>
            </div>
        </div>
    );
}
