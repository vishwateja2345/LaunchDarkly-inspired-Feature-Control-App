import { useCallback, useEffect, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { approvalsApi } from "./approvals.api.js";
import { useAuth } from "../../shared/AuthContext.jsx";
import { useWorkspace } from "../../shared/WorkspaceContext.jsx";
import { useToast } from "../../shared/components/Toast.jsx";
import { Modal } from "../../shared/components/Modal.jsx";
import { ConfirmationDialog } from "../../shared/components/ConfirmationDialog.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { EmptyState } from "../../shared/components/EmptyState.jsx";
import { Spinner } from "../../shared/components/Spinner.jsx";

const STATUS_TABS = [
    { key: "all", label: "All", status: undefined },
    { key: "pending", label: "Pending", status: "pending" },
    { key: "scheduled", label: "Scheduled", status: "scheduled" },
    { key: "applied", label: "Applied", status: "applied" },
    { key: "rejected", label: "Rejected", status: "rejected" },
    { key: "cancelled", label: "Cancelled", status: "cancelled" },
];

const STATUS_TONES = {
    pending: "warning",
    scheduled: "info",
    applied: "success",
    rejected: "danger",
    cancelled: "neutral",
};

const fieldStyle = {
    display: "grid",
    gap: 6,
    marginBottom: 14,
};

const labelStyle = {
    fontSize: 13,
    fontWeight: 600,
};

const inputStyle = {
    width: "100%",
    border: "1px solid var(--border-strong)",
    borderRadius: 8,
    padding: "10px 12px",
    background: "var(--field)",
    color: "var(--ink)",
    font: "inherit",
    boxSizing: "border-box",
};

const helperStyle = {
    margin: 0,
    color: "var(--muted)",
    fontSize: 12.5,
};

const detailListStyle = {
    margin: "8px 0 0",
    paddingLeft: 18,
    color: "var(--muted)",
    fontSize: 13.5,
};

const detailItemStyle = {
    marginBottom: 4,
};

const sectionTitleStyle = {
    margin: "12px 0 0",
    fontSize: 13.5,
};

function formatDateTime(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString();
}

function formatStatus(status) {
    if (!status) return "Unknown";
    return status.charAt(0).toUpperCase() + status.slice(1);
}

function formatCount(count, singular, plural) {
    return `${count} ${count === 1 ? singular : plural}`;
}

function getErrorMessage(caught) {
    if (!caught) return "Something went wrong.";
    if (caught.code === "SELF_APPROVAL_FORBIDDEN") {
        return "You cannot review your own approval request.";
    }
    if (caught.code === "APPROVAL_NOT_PENDING") {
        return "This approval request is no longer awaiting review.";
    }
    if (caught.code === "NOT_REQUESTER") {
        return "Only the person who created this request can cancel it.";
    }
    return caught.message || "Something went wrong.";
}

function isProductionEnvironment(request) {
    return request.environmentKey === "production" || request.environmentName?.toLowerCase() === "production";
}

function summarizeProposedChange(proposedChange = {}) {
    const lines = [proposedChange.enabled ? "Turn ON the flag." : "Turn OFF the flag."];

    if (Array.isArray(proposedChange.fallthroughRollout) && proposedChange.fallthroughRollout.length > 0) {
        const rollout = proposedChange.fallthroughRollout
            .map((entry) => `${entry.variationId}: ${entry.weight}%`)
            .join(", ");
        lines.push(`Serve a rollout by default (${rollout}).`);
    } else if (proposedChange.fallthroughVariationId) {
        lines.push(`Always serve variation ${proposedChange.fallthroughVariationId} by default.`);
    }

    const scopedChanges = [];
    if (Array.isArray(proposedChange.rules) && proposedChange.rules.length > 0) {
        scopedChanges.push(formatCount(proposedChange.rules.length, "targeting rule", "targeting rules"));
    }
    if (Array.isArray(proposedChange.targets) && proposedChange.targets.length > 0) {
        scopedChanges.push(formatCount(proposedChange.targets.length, "individual target", "individual targets"));
    }
    if (scopedChanges.length > 0) {
        lines.push(`Includes ${scopedChanges.join(" and ")}.`);
    }

    return lines;
}

function ApprovalCard({ request, isOwnRequest, onApprove, onReject, onCancel }) {
    const showReviewActions = request.status === "pending" && !isOwnRequest;
    const showScheduledReject = request.status === "scheduled" && !isOwnRequest;
    const showCancelAction = (request.status === "pending" || request.status === "scheduled") && isOwnRequest;

    return (
        <article className="approval-card">
            <header className="approval-card-header">
                <div>
                    <h3 style={{ margin: 0, fontSize: 18 }}>{request.flagName}</h3>
                    <div className="approval-card-meta">
                        <span>Requested by {request.requestedByName || "Unknown teammate"}</span>
                        <span>{formatDateTime(request.createdAt)}</span>
                    </div>
                </div>
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap", justifyContent: "flex-end" }}>
                    <Badge tone={isProductionEnvironment(request) ? "danger" : "neutral"}>{request.environmentName}</Badge>
                    <Badge tone={STATUS_TONES[request.status] || "neutral"}>{formatStatus(request.status)}</Badge>
                </div>
            </header>

            {request.reason && <div className="approval-card-reason">{request.reason}</div>}

            <div>
                <p style={sectionTitleStyle}><strong>Proposed change</strong></p>
                <ul style={detailListStyle}>
                    {summarizeProposedChange(request.proposedChange).map((line) => (
                        <li key={line} style={detailItemStyle}>{line}</li>
                    ))}
                </ul>
            </div>

            {request.status === "scheduled" && request.scheduledFor && (
                <p style={{ margin: "12px 0 0", fontSize: 13.5 }}>
                    <strong>Scheduled to apply on:</strong> {formatDateTime(request.scheduledFor)}
                </p>
            )}

            {request.reviewerName && (
                <div style={{ marginTop: 12 }}>
                    <p style={{ margin: 0, fontSize: 13.5 }}>
                        <strong>Reviewed by {request.reviewerName}</strong>
                        {request.reviewedAt ? ` on ${formatDateTime(request.reviewedAt)}` : ""}
                    </p>
                    {request.reviewComment && <p style={{ margin: "6px 0 0", color: "var(--muted)", fontSize: 13.5 }}>{request.reviewComment}</p>}
                </div>
            )}

            {(showReviewActions || showScheduledReject || showCancelAction) && (
                <div className="approval-card-actions">
                    {showReviewActions && (
                        <>
                            <button className="primary-button" type="button" onClick={() => onApprove(request)}>
                                Approve
                            </button>
                            <button className="danger-button" type="button" onClick={() => onReject(request)}>
                                Reject
                            </button>
                        </>
                    )}
                    {showScheduledReject && (
                        <button className="danger-button" type="button" onClick={() => onReject(request)}>
                            Reject scheduled change
                        </button>
                    )}
                    {showCancelAction && (
                        <>
                            <p className="self-request-note">
                                {request.status === "scheduled"
                                    ? "You requested this change, so someone else would need to reject it. You can cancel it instead."
                                    : "Waiting on someone else to review — you can cancel it instead."}
                            </p>
                            <button className="secondary-button" type="button" onClick={() => onCancel(request)}>
                                {request.status === "scheduled" ? "Cancel scheduled change" : "Cancel request"}
                            </button>
                        </>
                    )}
                </div>
            )}
        </article>
    );
}

function ApproveDialog({ request, onClose, onSubmit }) {
    const titleId = `approve-${useId().replace(/:/g, "")}`;
    const commentId = `approve-comment-${useId().replace(/:/g, "")}`;
    const scheduleId = `approve-schedule-${useId().replace(/:/g, "")}`;
    const [comment, setComment] = useState("");
    const [scheduleFor, setScheduleFor] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    const submit = async (event) => {
        event.preventDefault();
        setBusy(true);
        setError("");
        try {
            await onSubmit({
                comment: comment.trim(),
                scheduleFor: scheduleFor ? new Date(scheduleFor).toISOString() : null,
            });
            onClose();
        } catch (caught) {
            setError(getErrorMessage(caught));
        } finally {
            setBusy(false);
        }
    };

    return createPortal(
        <Modal labelledBy={titleId} onClose={() => !busy && onClose()}>
            <form className="form-dialog" onSubmit={submit}>
                <h2 id={titleId}>Approve {request.flagName}</h2>
                <p style={{ margin: "0 0 18px", color: "var(--muted)", fontSize: 13.5 }}>
                    Approve this production change now, or choose a future time to schedule it.
                </p>

                <div style={fieldStyle}>
                    <label htmlFor={commentId} style={labelStyle}>Comment (optional)</label>
                    <textarea
                        data-autofocus
                        disabled={busy}
                        id={commentId}
                        rows={4}
                        style={{ ...inputStyle, resize: "vertical" }}
                        value={comment}
                        onChange={(event) => setComment(event.target.value)}
                    />
                </div>

                <div style={fieldStyle}>
                    <label htmlFor={scheduleId} style={labelStyle}>Schedule for (optional)</label>
                    <input
                        disabled={busy}
                        id={scheduleId}
                        style={inputStyle}
                        type="datetime-local"
                        value={scheduleFor}
                        onChange={(event) => setScheduleFor(event.target.value)}
                    />
                    <p style={helperStyle}>Leave this blank to apply the change immediately.</p>
                </div>

                {error && <p className="inline-error" role="alert">{error}</p>}

                <footer>
                    <button className="secondary-button" disabled={busy} type="button" onClick={onClose}>
                        Cancel
                    </button>
                    <button className="primary-button" disabled={busy} type="submit">
                        {busy ? "Saving…" : scheduleFor ? "Approve & schedule" : "Approve now"}
                    </button>
                </footer>
            </form>
        </Modal>,
        document.body,
    );
}

function RejectDialog({ request, onClose, onSubmit }) {
    const titleId = `reject-${useId().replace(/:/g, "")}`;
    const commentId = `reject-comment-${useId().replace(/:/g, "")}`;
    const [comment, setComment] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    const submit = async (event) => {
        event.preventDefault();
        if (!comment.trim()) {
            setError("Please add a brief reason before rejecting this request.");
            return;
        }
        setBusy(true);
        setError("");
        try {
            await onSubmit({ comment: comment.trim() });
            onClose();
        } catch (caught) {
            setError(getErrorMessage(caught));
        } finally {
            setBusy(false);
        }
    };

    return createPortal(
        <Modal labelledBy={titleId} onClose={() => !busy && onClose()}>
            <form className="form-dialog" onSubmit={submit}>
                <h2 id={titleId}>Reject {request.flagName}</h2>
                <p style={{ margin: "0 0 18px", color: "var(--muted)", fontSize: 13.5 }}>
                    Share why this production change should not move forward.
                </p>

                <div style={fieldStyle}>
                    <label htmlFor={commentId} style={labelStyle}>Reason</label>
                    <textarea
                        data-autofocus
                        disabled={busy}
                        id={commentId}
                        required
                        rows={4}
                        style={{ ...inputStyle, resize: "vertical" }}
                        value={comment}
                        onChange={(event) => setComment(event.target.value)}
                    />
                </div>

                {error && <p className="inline-error" role="alert">{error}</p>}

                <footer>
                    <button className="secondary-button" disabled={busy} type="button" onClick={onClose}>
                        Cancel
                    </button>
                    <button className="danger-button" disabled={busy} type="submit">
                        {busy ? "Saving…" : request.status === "scheduled" ? "Reject scheduled change" : "Reject request"}
                    </button>
                </footer>
            </form>
        </Modal>,
        document.body,
    );
}

function CancelDialog({ request, onClose, onConfirm }) {
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    const confirm = async () => {
        setBusy(true);
        setError("");
        try {
            await onConfirm();
            onClose();
        } catch (caught) {
            setError(getErrorMessage(caught));
        } finally {
            setBusy(false);
        }
    };

    return (
        <ConfirmationDialog
            busy={busy}
            confirmLabel={request.status === "scheduled" ? "Cancel scheduled change" : "Cancel request"}
            danger
            error={error}
            message="This will stop the change from being applied. You can submit a new request later if needed."
            title={request.status === "scheduled" ? `Cancel scheduled change for ${request.flagName}?` : `Cancel request for ${request.flagName}?`}
            onCancel={onClose}
            onConfirm={confirm}
        />
    );
}

export function ApprovalsPage() {
    const { account } = useAuth();
    const { project } = useWorkspace();
    const { show } = useToast();
    const [activeTab, setActiveTab] = useState("all");
    const [approvals, setApprovals] = useState([]);
    const [loading, setLoading] = useState(true);
    const [approveTarget, setApproveTarget] = useState(null);
    const [rejectTarget, setRejectTarget] = useState(null);
    const [cancelTarget, setCancelTarget] = useState(null);
    const loadSequence = useRef(0);
    const tabReferences = useRef([]);

    const activeStatus = STATUS_TABS.find((tab) => tab.key === activeTab)?.status;

    const focusTab = (index) => {
        const tab = STATUS_TABS[index];
        if (!tab) return;
        setActiveTab(tab.key);
        window.requestAnimationFrame(() => tabReferences.current[index]?.focus());
    };

    const loadApprovals = useCallback(
        async (showLoading = true) => {
            if (!project?._id) {
                setApprovals([]);
                setLoading(false);
                return;
            }

            const sequence = loadSequence.current + 1;
            loadSequence.current = sequence;

            if (showLoading) setLoading(true);
            try {
                const data = await approvalsApi.list(project._id, activeStatus);
                if (loadSequence.current === sequence) {
                    setApprovals(Array.isArray(data) ? data : []);
                }
            } catch (caught) {
                if (loadSequence.current === sequence) {
                    setApprovals([]);
                    show(getErrorMessage(caught), { tone: "error" });
                }
            } finally {
                if (loadSequence.current === sequence) {
                    setLoading(false);
                }
            }
        },
        [activeStatus, project, show],
    );

    useEffect(() => {
        loadApprovals();
    }, [loadApprovals]);

    const handleApprove = async (values) => {
        try {
            await approvalsApi.approve(approveTarget._id, {
                comment: values.comment,
                ...(values.scheduleFor ? { scheduleFor: values.scheduleFor } : {}),
            });
            await loadApprovals(false);
            show(values.scheduleFor ? "Approval scheduled successfully." : "Approval applied successfully.", { tone: "success" });
        } catch (caught) {
            show(getErrorMessage(caught), { tone: "error" });
            throw caught;
        }
    };

    const handleReject = async (values) => {
        try {
            await approvalsApi.reject(rejectTarget._id, values);
            await loadApprovals(false);
            show("Approval request rejected.", { tone: "success" });
        } catch (caught) {
            show(getErrorMessage(caught), { tone: "error" });
            throw caught;
        }
    };

    const handleCancel = async () => {
        try {
            await approvalsApi.cancel(cancelTarget._id);
            await loadApprovals(false);
            show(cancelTarget.status === "scheduled" ? "Scheduled change cancelled." : "Approval request cancelled.", { tone: "success" });
        } catch (caught) {
            show(getErrorMessage(caught), { tone: "error" });
            throw caught;
        }
    };

    if (!project) {
        return <Spinner label="Loading approval requests" />;
    }

    return (
        <section>
            <header className="page-header">
                <div>
                    <h2>Approvals</h2>
                    <p>Review production flag changes, schedule approved rollouts, and track which teammate requested each update.</p>
                </div>
            </header>

            <div aria-label="Approval statuses" className="tab-bar" role="tablist">
                {STATUS_TABS.map((tab, index) => {
                    const active = tab.key === activeTab;
                    return (
                        <button
                            ref={(node) => {
                                tabReferences.current[index] = node;
                            }}
                            key={tab.key}
                            aria-selected={active}
                            className={`tab-item ${active ? "tab-item-active" : ""}`}
                            role="tab"
                            tabIndex={0}
                            type="button"
                            onKeyDown={(event) => {
                                if (event.key === "ArrowRight") {
                                    event.preventDefault();
                                    focusTab((index + 1) % STATUS_TABS.length);
                                } else if (event.key === "ArrowLeft") {
                                    event.preventDefault();
                                    focusTab((index - 1 + STATUS_TABS.length) % STATUS_TABS.length);
                                } else if (event.key === "Home") {
                                    event.preventDefault();
                                    focusTab(0);
                                } else if (event.key === "End") {
                                    event.preventDefault();
                                    focusTab(STATUS_TABS.length - 1);
                                }
                            }}
                            onClick={() => setActiveTab(tab.key)}
                        >
                            {tab.label}
                        </button>
                    );
                })}
            </div>

            <div aria-live="polite" role="tabpanel">
                {loading ? (
                    <Spinner label="Loading approval requests" />
                ) : approvals.length === 0 ? (
                    <EmptyState
                        description="No approval requests match this filter right now."
                        icon="approval"
                        title="Nothing to review"
                    />
                ) : (
                    approvals.map((request) => (
                        <ApprovalCard
                            key={request._id}
                            isOwnRequest={account?._id === request.requestedBy}
                            request={request}
                            onApprove={setApproveTarget}
                            onCancel={setCancelTarget}
                            onReject={setRejectTarget}
                        />
                    ))
                )}
            </div>

            {approveTarget && (
                <ApproveDialog
                    request={approveTarget}
                    onClose={() => setApproveTarget(null)}
                    onSubmit={handleApprove}
                />
            )}

            {rejectTarget && (
                <RejectDialog
                    request={rejectTarget}
                    onClose={() => setRejectTarget(null)}
                    onSubmit={handleReject}
                />
            )}

            {cancelTarget && (
                <CancelDialog
                    request={cancelTarget}
                    onClose={() => setCancelTarget(null)}
                    onConfirm={handleCancel}
                />
            )}
        </section>
    );
}
