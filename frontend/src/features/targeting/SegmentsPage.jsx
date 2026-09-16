import { useCallback, useEffect, useId, useState } from "react";
import { createPortal } from "react-dom";
import { appUsersApi, segmentsApi } from "./targeting.api.js";
import { useWorkspace } from "../../shared/WorkspaceContext.jsx";
import { useToast } from "../../shared/components/Toast.jsx";
import { Modal } from "../../shared/components/Modal.jsx";
import { ConfirmationDialog } from "../../shared/components/ConfirmationDialog.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { EmptyState } from "../../shared/components/EmptyState.jsx";
import { Spinner } from "../../shared/components/Spinner.jsx";
import { fieldErrorsFrom as extractFieldErrors, firstErrorForPrefix } from "../../shared/fieldErrors.js";

const SEGMENT_OPERATOR_OPTIONS = [
    { value: "equals", label: "is" },
    { value: "notEquals", label: "is not" },
    { value: "in", label: "is any of" },
    { value: "notIn", label: "is none of" },
    { value: "contains", label: "contains" },
    { value: "greaterThan", label: "is greater than" },
    { value: "lessThan", label: "is less than" },
];

const DEFAULT_RULE = {
    attribute: "",
    operator: "equals",
    valuesText: "",
};

const DEFAULT_ATTRIBUTE_ROW = {
    key: "",
    value: "",
};

export function SegmentsPage() {
    const { project } = useWorkspace();
    const { show } = useToast();
    const projectId = project?._id || null;

    const [activeTab, setActiveTab] = useState("segments");

    const [segments, setSegments] = useState([]);
    const [segmentsLoading, setSegmentsLoading] = useState(true);
    const [segmentsError, setSegmentsError] = useState("");
    const [segmentDialogState, setSegmentDialogState] = useState(null);
    const [segmentToDelete, setSegmentToDelete] = useState(null);
    const [segmentDeleteBusy, setSegmentDeleteBusy] = useState(false);
    const [segmentDeleteError, setSegmentDeleteError] = useState("");
    const [previewBySegmentId, setPreviewBySegmentId] = useState({});
    const [previewLoadingBySegmentId, setPreviewLoadingBySegmentId] = useState({});

    const [searchInput, setSearchInput] = useState("");
    const [search, setSearch] = useState("");
    const [users, setUsers] = useState([]);
    const [usersLoading, setUsersLoading] = useState(true);
    const [usersError, setUsersError] = useState("");
    const [userDialogState, setUserDialogState] = useState(null);
    const [userToDelete, setUserToDelete] = useState(null);
    const [userDeleteBusy, setUserDeleteBusy] = useState(false);
    const [userDeleteError, setUserDeleteError] = useState("");

    const loadSegments = useCallback(async () => {
        if (!projectId) {
            setSegments([]);
            setSegmentsLoading(false);
            setSegmentsError("");
            return;
        }

        setSegmentsLoading(true);
        setSegmentsError("");
        try {
            const data = await segmentsApi.list(projectId);
            setSegments(Array.isArray(data) ? data : []);
            setPreviewBySegmentId({});
        } catch (caught) {
            setSegmentsError(caught.message || "Unable to load segments.");
        } finally {
            setSegmentsLoading(false);
        }
    }, [projectId]);

    const loadUsers = useCallback(
        async (nextSearch = search) => {
            if (!projectId) {
                setUsers([]);
                setUsersLoading(false);
                setUsersError("");
                return;
            }

            setUsersLoading(true);
            setUsersError("");
            try {
                const data = await appUsersApi.list(projectId, nextSearch.trim());
                setUsers(Array.isArray(data) ? data : []);
            } catch (caught) {
                setUsersError(caught.message || "Unable to load audience users.");
            } finally {
                setUsersLoading(false);
            }
        },
        [projectId, search],
    );

    useEffect(() => {
        loadSegments();
    }, [loadSegments]);

    useEffect(() => {
        const timeoutId = window.setTimeout(() => {
            setSearch(searchInput.trim());
        }, 300);

        return () => window.clearTimeout(timeoutId);
    }, [searchInput]);

    useEffect(() => {
        loadUsers(search);
    }, [loadUsers, search]);

    const handlePreviewSegment = async (segmentId) => {
        setPreviewLoadingBySegmentId((current) => ({ ...current, [segmentId]: true }));
        try {
            const preview = await segmentsApi.preview(segmentId);
            setPreviewBySegmentId((current) => ({
                ...current,
                [segmentId]: preview?.matchingUserCount ?? 0,
            }));
        } catch (caught) {
            show(caught.message || "Unable to preview segment matches.", { tone: "error" });
        } finally {
            setPreviewLoadingBySegmentId((current) => ({ ...current, [segmentId]: false }));
        }
    };

    const handleDeleteSegment = async () => {
        if (!segmentToDelete) return;

        setSegmentDeleteBusy(true);
        setSegmentDeleteError("");
        try {
            await segmentsApi.remove(segmentToDelete._id);
            setSegmentToDelete(null);
            await loadSegments();
            show(`Deleted segment "${segmentToDelete.name}".`, { tone: "success" });
        } catch (caught) {
            setSegmentDeleteError(caught.message || "Unable to delete segment.");
        } finally {
            setSegmentDeleteBusy(false);
        }
    };

    const handleDeleteUser = async () => {
        if (!userToDelete) return;

        setUserDeleteBusy(true);
        setUserDeleteError("");
        try {
            await appUsersApi.remove(userToDelete._id);
            setUserToDelete(null);
            await loadUsers();
            show(`Deleted user "${userToDelete.key}".`, { tone: "success" });
        } catch (caught) {
            setUserDeleteError(caught.message || "Unable to delete user.");
        } finally {
            setUserDeleteBusy(false);
        }
    };

    if (!project) {
        return <Spinner label="Loading workspace" />;
    }

    return (
        <section>
            <header className="page-header">
                <div>
                    <h2>Targeting</h2>
                    <p>Manage reusable audience segments and the end users your flags evaluate against.</p>
                </div>
            </header>

            <div className="tab-bar" role="tablist" aria-label="Targeting views">
                <button
                    className={`tab-item ${activeTab === "segments" ? "tab-item-active" : ""}`}
                    role="tab"
                    type="button"
                    aria-selected={activeTab === "segments"}
                    onClick={() => setActiveTab("segments")}
                >
                    Segments
                </button>
                <button
                    className={`tab-item ${activeTab === "audience" ? "tab-item-active" : ""}`}
                    role="tab"
                    type="button"
                    aria-selected={activeTab === "audience"}
                    onClick={() => setActiveTab("audience")}
                >
                    Audience
                </button>
            </div>

            {activeTab === "segments" ? (
                <SegmentsTab
                    previewBySegmentId={previewBySegmentId}
                    previewLoadingBySegmentId={previewLoadingBySegmentId}
                    segments={segments}
                    segmentsError={segmentsError}
                    segmentsLoading={segmentsLoading}
                    onDeleteSegment={setSegmentToDelete}
                    onEditSegment={(segment) => setSegmentDialogState({ mode: "edit", segment })}
                    onNewSegment={() => setSegmentDialogState({ mode: "create", segment: null })}
                    onPreviewSegment={handlePreviewSegment}
                />
            ) : (
                <AudienceTab
                    searchInput={searchInput}
                    searchTerm={search}
                    users={users}
                    usersError={usersError}
                    usersLoading={usersLoading}
                    onDeleteUser={setUserToDelete}
                    onEditUser={(user) => setUserDialogState({ mode: "edit", user })}
                    onNewUser={() => setUserDialogState({ mode: "create", user: null })}
                    onSearchChange={setSearchInput}
                />
            )}

            {segmentDialogState && (
                <SegmentFormDialog
                    mode={segmentDialogState.mode}
                    projectId={projectId}
                    segment={segmentDialogState.segment}
                    onClose={() => setSegmentDialogState(null)}
                    onSaved={async (message) => {
                        setSegmentDialogState(null);
                        await loadSegments();
                        show(message, { tone: "success" });
                    }}
                />
            )}

            {userDialogState && (
                <UserFormDialog
                    mode={userDialogState.mode}
                    projectId={projectId}
                    user={userDialogState.user}
                    onClose={() => setUserDialogState(null)}
                    onSaved={async (message) => {
                        setUserDialogState(null);
                        await loadUsers();
                        show(message, { tone: "success" });
                    }}
                />
            )}

            {segmentToDelete && (
                <ConfirmationDialog
                    busy={segmentDeleteBusy}
                    confirmLabel="Delete segment"
                    danger
                    error={segmentDeleteError}
                    message={`Flags referencing the segment key "${segmentToDelete.key}" in targeting rules will stop matching it. This does not delete or rewrite those flag rules.`}
                    title={`Delete ${segmentToDelete.name}?`}
                    onCancel={() => {
                        if (segmentDeleteBusy) return;
                        setSegmentToDelete(null);
                        setSegmentDeleteError("");
                    }}
                    onConfirm={handleDeleteSegment}
                />
            )}

            {userToDelete && (
                <ConfirmationDialog
                    busy={userDeleteBusy}
                    confirmLabel="Delete user"
                    danger
                    error={userDeleteError}
                    message={`Delete audience user "${userToDelete.key}"? Existing evaluations for that user key will no longer find this stored profile.`}
                    title={`Delete ${userToDelete.name || userToDelete.key}?`}
                    onCancel={() => {
                        if (userDeleteBusy) return;
                        setUserToDelete(null);
                        setUserDeleteError("");
                    }}
                    onConfirm={handleDeleteUser}
                />
            )}
        </section>
    );
}

function SegmentsTab({
    previewBySegmentId,
    previewLoadingBySegmentId,
    segments,
    segmentsError,
    segmentsLoading,
    onDeleteSegment,
    onEditSegment,
    onNewSegment,
    onPreviewSegment,
}) {
    return (
        <section role="tabpanel" aria-label="Segments">
            <div className="page-toolbar">
                <button className="primary-button" type="button" onClick={onNewSegment}>
                    New segment
                </button>
            </div>

            {segmentsLoading ? (
                <Spinner label="Loading segments" />
            ) : segmentsError ? (
                <div className="inline-error" role="alert">
                    {segmentsError}
                </div>
            ) : segments.length === 0 ? (
                <EmptyState
                    action={
                        <button className="primary-button" type="button" onClick={onNewSegment}>
                            New segment
                        </button>
                    }
                    description="Create reusable user groups for targeting rules, rollouts, and experiments."
                    icon="group"
                    title="No segments yet"
                />
            ) : (
                <div className="card-list">
                    {segments.map((segment) => {
                        const previewCount = previewBySegmentId[segment._id];
                        const previewLoading = previewLoadingBySegmentId[segment._id];
                        return (
                            <article className="card" key={segment._id}>
                                <div
                                    style={{
                                        display: "flex",
                                        justifyContent: "space-between",
                                        gap: 16,
                                        alignItems: "flex-start",
                                        flexWrap: "wrap",
                                    }}
                                >
                                    <div style={{ minWidth: 0, flex: "1 1 320px" }}>
                                        <div
                                            style={{
                                                display: "flex",
                                                gap: 8,
                                                alignItems: "center",
                                                flexWrap: "wrap",
                                                marginBottom: 6,
                                            }}
                                        >
                                            <h3 style={{ margin: 0, fontSize: 16 }}>{segment.name}</h3>
                                            <Badge tone="neutral">{segment.key}</Badge>
                                        </div>
                                        {segment.description ? (
                                            <p style={{ margin: "0 0 10px", color: "var(--muted)", fontSize: 13.5 }}>
                                                {segment.description}
                                            </p>
                                        ) : (
                                            <p style={{ margin: "0 0 10px", color: "var(--faint)", fontSize: 13 }}>
                                                No description
                                            </p>
                                        )}
                                        <p style={{ margin: "0 0 12px", fontSize: 13.5 }}>
                                            <strong>Rules:</strong> {summarizeRules(segment.rules)}
                                        </p>
                                        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                                            <Badge tone="info">Included {segment.includedKeys?.length || 0}</Badge>
                                            <Badge tone="warning">Excluded {segment.excludedKeys?.length || 0}</Badge>
                                            {typeof previewCount === "number" && (
                                                <Badge tone="success">{formatMatchingCount(previewCount)}</Badge>
                                            )}
                                        </div>
                                    </div>

                                    <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                                        <button
                                            className="secondary-button"
                                            disabled={previewLoading}
                                            type="button"
                                            onClick={() => onPreviewSegment(segment._id)}
                                        >
                                            {previewLoading ? "Previewing…" : "Preview match"}
                                        </button>
                                        <button
                                            className="icon-button"
                                            type="button"
                                            aria-label={`Edit ${segment.name}`}
                                            title="Edit segment"
                                            onClick={() => onEditSegment(segment)}
                                        >
                                            <MaterialIcon size={18}>edit</MaterialIcon>
                                        </button>
                                        <button
                                            className="icon-button"
                                            type="button"
                                            aria-label={`Delete ${segment.name}`}
                                            title="Delete segment"
                                            onClick={() => onDeleteSegment(segment)}
                                        >
                                            <MaterialIcon size={18}>delete</MaterialIcon>
                                        </button>
                                    </div>
                                </div>
                            </article>
                        );
                    })}
                </div>
            )}
        </section>
    );
}

function AudienceTab({
    searchInput,
    searchTerm,
    users,
    usersError,
    usersLoading,
    onDeleteUser,
    onEditUser,
    onNewUser,
    onSearchChange,
}) {
    const hasSearch = Boolean(searchTerm);

    return (
        <section role="tabpanel" aria-label="Audience">
            <div className="page-toolbar" style={{ justifyContent: "space-between" }}>
                <label className="search-input" style={{ flex: "1 1 320px", maxWidth: 420 }}>
                    <MaterialIcon size={18}>search</MaterialIcon>
                    <input
                        aria-label="Search audience users"
                        placeholder="Search by key, name, email, or attributes"
                        type="search"
                        value={searchInput}
                        onChange={(event) => onSearchChange(event.target.value)}
                    />
                </label>
                <button className="primary-button" type="button" onClick={onNewUser}>
                    New user
                </button>
            </div>

            {usersLoading ? (
                <Spinner label="Loading audience users" />
            ) : usersError ? (
                <div className="inline-error" role="alert">
                    {usersError}
                </div>
            ) : users.length === 0 ? (
                <EmptyState
                    action={
                        !hasSearch ? (
                            <button className="primary-button" type="button" onClick={onNewUser}>
                                New user
                            </button>
                        ) : null
                    }
                    description={
                        hasSearch
                            ? `No audience users match “${searchTerm}”. Try a different search.`
                            : "Create audience users so segments and targeting rules can evaluate real user context."
                    }
                    icon="person"
                    title={hasSearch ? "No matching users" : "No audience users yet"}
                />
            ) : (
                <div className="card" style={{ overflowX: "auto" }}>
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th scope="col">Key</th>
                                <th scope="col">Name</th>
                                <th scope="col">Email</th>
                                <th scope="col">Plan</th>
                                <th scope="col">Country</th>
                                <th scope="col">Attributes</th>
                                <th scope="col" style={{ width: 96 }}>
                                    Actions
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((user) => (
                                <tr key={user._id}>
                                    <td>
                                        <code>{user.key}</code>
                                    </td>
                                    <td>{user.name || "—"}</td>
                                    <td>{user.email || "—"}</td>
                                    <td>{user.plan || "—"}</td>
                                    <td>{user.country || "—"}</td>
                                    <td>
                                        {renderAttributeChips(user.attributes)}
                                    </td>
                                    <td>
                                        <div style={{ display: "flex", gap: 4 }}>
                                            <button
                                                className="icon-button"
                                                type="button"
                                                aria-label={`Edit ${user.key}`}
                                                title="Edit user"
                                                onClick={() => onEditUser(user)}
                                            >
                                                <MaterialIcon size={18}>edit</MaterialIcon>
                                            </button>
                                            <button
                                                className="icon-button"
                                                type="button"
                                                aria-label={`Delete ${user.key}`}
                                                title="Delete user"
                                                onClick={() => onDeleteUser(user)}
                                            >
                                                <MaterialIcon size={18}>delete</MaterialIcon>
                                            </button>
                                        </div>
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

function SegmentFormDialog({ mode, projectId, segment, onClose, onSaved }) {
    const titleId = `segment-dialog-${useId().replace(/:/g, "")}`;
    const [name, setName] = useState(segment?.name || "");
    const [keyValue, setKeyValue] = useState(segment?.key || "");
    const [description, setDescription] = useState(segment?.description || "");
    const [rules, setRules] = useState(() => createRuleDrafts(segment?.rules));
    const [includedKeysText, setIncludedKeysText] = useState((segment?.includedKeys || []).join(", "));
    const [excludedKeysText, setExcludedKeysText] = useState((segment?.excludedKeys || []).join(", "));
    const [fieldErrors, setFieldErrors] = useState({});
    const [formError, setFormError] = useState("");
    const [saving, setSaving] = useState(false);

    const submit = async (event) => {
        event.preventDefault();

        const nextFieldErrors = {};
        const trimmedName = name.trim();
        const trimmedKey = keyValue.trim();
        const normalizedRules = normalizeRules(rules);

        if (!trimmedName) nextFieldErrors.name = "Name is required.";
        if (normalizedRules.some((rule) => !rule.attribute || rule.values.length === 0)) {
            nextFieldErrors.rules = "Each rule must include an attribute and at least one value.";
        }

        if (Object.keys(nextFieldErrors).length > 0) {
            setFieldErrors(nextFieldErrors);
            setFormError("");
            return;
        }

        const payload = {
            name: trimmedName,
            description: description.trim(),
            rules: normalizedRules,
            includedKeys: parseKeyList(includedKeysText),
            excludedKeys: parseKeyList(excludedKeysText),
        };

        if (trimmedKey) payload.key = trimmedKey;

        setSaving(true);
        setFieldErrors({});
        setFormError("");
        try {
            if (mode === "create") {
                await segmentsApi.create(projectId, payload);
                await onSaved(`Created segment "${trimmedName}".`);
            } else {
                await segmentsApi.update(segment._id, payload);
                await onSaved(`Updated segment "${trimmedName}".`);
            }
        } catch (caught) {
            setFieldErrors(extractFieldErrors(caught));
            setFormError(caught.message || `Unable to ${mode === "create" ? "create" : "update"} segment.`);
        } finally {
            setSaving(false);
        }
    };

    return createPortal(
        <Modal className="form-dialog" labelledBy={titleId} onClose={() => !saving && onClose()}>
            <form onSubmit={submit}>
                <h2 id={titleId}>{mode === "create" ? "New segment" : "Edit segment"}</h2>

                {formError && (
                    <div className="inline-error" role="alert" style={{ marginBottom: 16 }}>
                        {formError}
                    </div>
                )}

                <div className="field-group">
                    <label htmlFor={`${titleId}-name`}>Name</label>
                    <input
                        data-autofocus
                        disabled={saving}
                        id={`${titleId}-name`}
                        required
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                    />
                    {fieldErrors.name && (
                        <div className="field-error" role="alert">
                            {fieldErrors.name}
                        </div>
                    )}
                </div>

                <div className="field-group">
                    <label htmlFor={`${titleId}-key`}>Key</label>
                    <input
                        disabled={saving}
                        id={`${titleId}-key`}
                        type="text"
                        value={keyValue}
                        onChange={(event) => setKeyValue(event.target.value)}
                    />
                    <div className="field-hint">
                        Leave blank on create to let the server generate a key from the name.
                    </div>
                    {fieldErrors.key && (
                        <div className="field-error" role="alert">
                            {fieldErrors.key}
                        </div>
                    )}
                </div>

                <div className="field-group">
                    <label htmlFor={`${titleId}-description`}>Description</label>
                    <textarea
                        disabled={saving}
                        id={`${titleId}-description`}
                        rows={3}
                        value={description}
                        onChange={(event) => setDescription(event.target.value)}
                    />
                    {fieldErrors.description && (
                        <div className="field-error" role="alert">
                            {fieldErrors.description}
                        </div>
                    )}
                </div>

                <div className="field-group">
                    <label>Rules</label>
                    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                        {rules.map((rule, index) => (
                            <RuleRow
                                key={`${titleId}-rule-${index}`}
                                disabled={saving}
                                index={index}
                                rule={rule}
                                canRemove={rules.length > 1}
                                onChange={(nextRule) => {
                                    setRules((current) => current.map((entry, entryIndex) => (entryIndex === index ? nextRule : entry)));
                                }}
                                onRemove={() => {
                                    setRules((current) => current.filter((_, entryIndex) => entryIndex !== index));
                                }}
                            />
                        ))}
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                        <div className="field-hint">
                            Rules are AND-ed together. Values accept comma-separated entries.
                        </div>
                        <button
                            className="secondary-button"
                            disabled={saving}
                            type="button"
                            onClick={() => setRules((current) => [...current, { ...DEFAULT_RULE }])}
                        >
                            Add rule
                        </button>
                    </div>
                    {firstErrorForPrefix(fieldErrors, "rules") && (
                        <div className="field-error" role="alert" style={{ margin: "6px 0 0" }}>
                            {firstErrorForPrefix(fieldErrors, "rules")}
                        </div>
                    )}
                </div>

                <div className="field-group">
                    <label htmlFor={`${titleId}-included`}>Included user keys</label>
                    <textarea
                        disabled={saving}
                        id={`${titleId}-included`}
                        rows={2}
                        value={includedKeysText}
                        onChange={(event) => setIncludedKeysText(event.target.value)}
                    />
                    <div className="field-hint">Comma or whitespace separated user keys to always include.</div>
                    {fieldErrors.includedKeys && (
                        <div className="field-error" role="alert">
                            {fieldErrors.includedKeys}
                        </div>
                    )}
                </div>

                <div className="field-group">
                    <label htmlFor={`${titleId}-excluded`}>Excluded user keys</label>
                    <textarea
                        disabled={saving}
                        id={`${titleId}-excluded`}
                        rows={2}
                        value={excludedKeysText}
                        onChange={(event) => setExcludedKeysText(event.target.value)}
                    />
                    <div className="field-hint">Comma or whitespace separated user keys to always exclude.</div>
                    {fieldErrors.excludedKeys && (
                        <div className="field-error" role="alert">
                            {fieldErrors.excludedKeys}
                        </div>
                    )}
                </div>

                <footer>
                    <button className="secondary-button" disabled={saving} type="button" onClick={onClose}>
                        Cancel
                    </button>
                    <button className="primary-button" disabled={saving} type="submit">
                        {saving ? "Saving…" : mode === "create" ? "Create segment" : "Save changes"}
                    </button>
                </footer>
            </form>
        </Modal>,
        document.body,
    );
}

function RuleRow({ canRemove, disabled, index, rule, onChange, onRemove }) {
    return (
        <div className="card" style={{ padding: 16 }}>
            <div className="field-row">
                <div className="field-group" style={{ marginBottom: 0 }}>
                    <label htmlFor={`segment-rule-attribute-${index}`}>Attribute</label>
                    <input
                        disabled={disabled}
                        id={`segment-rule-attribute-${index}`}
                        placeholder="plan, country, betaOptIn…"
                        type="text"
                        value={rule.attribute}
                        onChange={(event) => onChange({ ...rule, attribute: event.target.value })}
                    />
                </div>
                <div className="field-group" style={{ marginBottom: 0 }}>
                    <label>Operator</label>
                    <SelectMenu
                        ariaLabel={`Operator for rule ${index + 1}`}
                        disabled={disabled}
                        options={SEGMENT_OPERATOR_OPTIONS}
                        value={rule.operator}
                        onChange={(value) => onChange({ ...rule, operator: value })}
                    />
                </div>
                <div className="field-group" style={{ marginBottom: 0 }}>
                    <label htmlFor={`segment-rule-values-${index}`}>Values</label>
                    <input
                        disabled={disabled}
                        id={`segment-rule-values-${index}`}
                        placeholder="enterprise, pro"
                        type="text"
                        value={rule.valuesText}
                        onChange={(event) => onChange({ ...rule, valuesText: event.target.value })}
                    />
                </div>
            </div>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center", marginTop: 10 }}>
                <span style={{ color: "var(--muted)", fontSize: 12.5 }}>Rule {index + 1}</span>
                <button className="icon-button" disabled={disabled || !canRemove} type="button" aria-label={`Remove rule ${index + 1}`} onClick={onRemove}>
                    <MaterialIcon size={18}>delete</MaterialIcon>
                </button>
            </div>
        </div>
    );
}

function UserFormDialog({ mode, projectId, user, onClose, onSaved }) {
    const titleId = `user-dialog-${useId().replace(/:/g, "")}`;
    const [keyValue, setKeyValue] = useState(user?.key || "");
    const [name, setName] = useState(user?.name || "");
    const [email, setEmail] = useState(user?.email || "");
    const [plan, setPlan] = useState(user?.plan || "");
    const [country, setCountry] = useState(user?.country || "");
    const [attributeRows, setAttributeRows] = useState(() => createAttributeRows(user?.attributes));
    const [fieldErrors, setFieldErrors] = useState({});
    const [formError, setFormError] = useState("");
    const [saving, setSaving] = useState(false);

    const submit = async (event) => {
        event.preventDefault();

        const nextFieldErrors = {};
        if (!keyValue.trim()) nextFieldErrors.key = "Key is required.";
        if (!name.trim()) nextFieldErrors.name = "Name is required.";
        if (attributeRows.some((row) => row.value.trim() && !row.key.trim())) {
            nextFieldErrors.attributes = "Add a name for every custom attribute value.";
        }

        if (Object.keys(nextFieldErrors).length > 0) {
            setFieldErrors(nextFieldErrors);
            setFormError("");
            return;
        }

        const payload = {
            key: keyValue.trim(),
            name: name.trim(),
            email: email.trim(),
            plan: plan.trim(),
            country: country.trim(),
            attributes: buildAttributesObject(attributeRows),
        };

        setSaving(true);
        setFieldErrors({});
        setFormError("");
        try {
            if (mode === "create") {
                await appUsersApi.create(projectId, payload);
                await onSaved(`Created audience user "${payload.key}".`);
            } else {
                await appUsersApi.update(user._id, payload);
                await onSaved(`Updated audience user "${payload.key}".`);
            }
        } catch (caught) {
            const nextErrors = extractFieldErrors(caught);
            if (caught.code === "USER_KEY_CONFLICT" && !nextErrors.key) {
                nextErrors.key = "That user key already exists in this project.";
            }
            setFieldErrors(nextErrors);
            setFormError(caught.message || `Unable to ${mode === "create" ? "create" : "update"} user.`);
        } finally {
            setSaving(false);
        }
    };

    return createPortal(
        <Modal className="form-dialog" labelledBy={titleId} onClose={() => !saving && onClose()}>
            <form onSubmit={submit}>
                <h2 id={titleId}>{mode === "create" ? "New audience user" : "Edit audience user"}</h2>

                {formError && (
                    <div className="inline-error" role="alert" style={{ marginBottom: 16 }}>
                        {formError}
                    </div>
                )}

                <div className="field-row">
                    <div className="field-group">
                        <label htmlFor={`${titleId}-key`}>Key</label>
                        <input
                            data-autofocus
                            disabled={saving}
                            id={`${titleId}-key`}
                            required
                            type="text"
                            value={keyValue}
                            onChange={(event) => setKeyValue(event.target.value)}
                        />
                        {fieldErrors.key && (
                            <div className="field-error" role="alert">
                                {fieldErrors.key}
                            </div>
                        )}
                    </div>
                    <div className="field-group">
                        <label htmlFor={`${titleId}-name`}>Name</label>
                        <input
                            disabled={saving}
                            id={`${titleId}-name`}
                            required
                            type="text"
                            value={name}
                            onChange={(event) => setName(event.target.value)}
                        />
                        {fieldErrors.name && (
                            <div className="field-error" role="alert">
                                {fieldErrors.name}
                            </div>
                        )}
                    </div>
                </div>

                <div className="field-row">
                    <div className="field-group">
                        <label htmlFor={`${titleId}-email`}>Email</label>
                        <input
                            disabled={saving}
                            id={`${titleId}-email`}
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                        />
                        {fieldErrors.email && (
                            <div className="field-error" role="alert">
                                {fieldErrors.email}
                            </div>
                        )}
                    </div>
                    <div className="field-group">
                        <label htmlFor={`${titleId}-plan`}>Plan</label>
                        <input
                            disabled={saving}
                            id={`${titleId}-plan`}
                            type="text"
                            value={plan}
                            onChange={(event) => setPlan(event.target.value)}
                        />
                        {fieldErrors.plan && (
                            <div className="field-error" role="alert">
                                {fieldErrors.plan}
                            </div>
                        )}
                    </div>
                </div>

                <div className="field-group">
                    <label htmlFor={`${titleId}-country`}>Country</label>
                    <input
                        disabled={saving}
                        id={`${titleId}-country`}
                        type="text"
                        value={country}
                        onChange={(event) => setCountry(event.target.value)}
                    />
                    {fieldErrors.country && (
                        <div className="field-error" role="alert">
                            {fieldErrors.country}
                        </div>
                    )}
                </div>

                <div className="field-group">
                    <label>Custom attributes</label>
                    <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                        {attributeRows.map((row, index) => (
                            <div className="field-row" key={`${titleId}-attribute-${index}`} style={{ alignItems: "flex-end" }}>
                                <div className="field-group" style={{ marginBottom: 0 }}>
                                    <label htmlFor={`${titleId}-attribute-key-${index}`}>Attribute key</label>
                                    <input
                                        disabled={saving}
                                        id={`${titleId}-attribute-key-${index}`}
                                        placeholder="betaOptIn"
                                        type="text"
                                        value={row.key}
                                        onChange={(event) => {
                                            setAttributeRows((current) =>
                                                current.map((entry, entryIndex) =>
                                                    entryIndex === index ? { ...entry, key: event.target.value } : entry,
                                                ),
                                            );
                                        }}
                                    />
                                </div>
                                <div className="field-group" style={{ marginBottom: 0 }}>
                                    <label htmlFor={`${titleId}-attribute-value-${index}`}>Value</label>
                                    <input
                                        disabled={saving}
                                        id={`${titleId}-attribute-value-${index}`}
                                        placeholder='true, 42, or "2026-q1"'
                                        type="text"
                                        value={row.value}
                                        onChange={(event) => {
                                            setAttributeRows((current) =>
                                                current.map((entry, entryIndex) =>
                                                    entryIndex === index ? { ...entry, value: event.target.value } : entry,
                                                ),
                                            );
                                        }}
                                    />
                                </div>
                                <button
                                    className="icon-button"
                                    disabled={saving || attributeRows.length === 1}
                                    type="button"
                                    aria-label={`Remove attribute row ${index + 1}`}
                                    onClick={() => {
                                        setAttributeRows((current) => current.filter((_, entryIndex) => entryIndex !== index));
                                    }}
                                >
                                    <MaterialIcon size={18}>delete</MaterialIcon>
                                </button>
                            </div>
                        ))}
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                        <div className="field-hint">
                            Boolean strings and numeric strings are converted automatically.
                        </div>
                        <button
                            className="secondary-button"
                            disabled={saving}
                            type="button"
                            onClick={() => setAttributeRows((current) => [...current, { ...DEFAULT_ATTRIBUTE_ROW }])}
                        >
                            Add attribute
                        </button>
                    </div>
                    {fieldErrors.attributes && (
                        <div className="field-error" role="alert" style={{ margin: "6px 0 0" }}>
                            {fieldErrors.attributes}
                        </div>
                    )}
                </div>

                <footer>
                    <button className="secondary-button" disabled={saving} type="button" onClick={onClose}>
                        Cancel
                    </button>
                    <button className="primary-button" disabled={saving} type="submit">
                        {saving ? "Saving…" : mode === "create" ? "Create user" : "Save changes"}
                    </button>
                </footer>
            </form>
        </Modal>,
        document.body,
    );
}

function createRuleDrafts(rules) {
    if (!Array.isArray(rules) || rules.length === 0) {
        return [{ ...DEFAULT_RULE }];
    }

    return rules.map((rule) => ({
        attribute: rule.attribute || "",
        operator: rule.operator || "equals",
        valuesText: Array.isArray(rule.values) ? rule.values.join(", ") : "",
    }));
}

function normalizeRules(rules) {
    return rules
        .map((rule) => ({
            attribute: rule.attribute.trim(),
            operator: rule.operator || "equals",
            values: parseCommaList(rule.valuesText),
        }))
        .filter((rule) => rule.attribute || rule.values.length > 0);
}

function parseCommaList(value) {
    return String(value || "")
        .split(",")
        .map((entry) => entry.trim())
        .filter(Boolean);
}

function parseKeyList(value) {
    return String(value || "")
        .split(/[\s,]+/)
        .map((entry) => entry.trim())
        .filter(Boolean);
}

function summarizeRules(rules) {
    if (!Array.isArray(rules) || rules.length === 0) {
        return "No rules defined";
    }

    return rules
        .map((rule) => {
            const attribute = rule.attribute || "attribute";
            const values = Array.isArray(rule.values) ? rule.values : [];
            const renderedValues = values.length > 1 ? `[${values.join(", ")}]` : values[0] || "—";

            switch (rule.operator) {
                case "equals":
                    return `${attribute} is ${renderedValues}`;
                case "notEquals":
                    return `${attribute} is not ${renderedValues}`;
                case "in":
                    return `${attribute} is any of ${renderedValues}`;
                case "notIn":
                    return `${attribute} is none of ${renderedValues}`;
                case "contains":
                    return `${attribute} contains ${renderedValues}`;
                case "greaterThan":
                    return `${attribute} is greater than ${renderedValues}`;
                case "lessThan":
                    return `${attribute} is less than ${renderedValues}`;
                default:
                    return `${attribute} ${rule.operator || "matches"} ${renderedValues}`;
            }
        })
        .join(" AND ");
}

function formatMatchingCount(count) {
    return `${count} user${count === 1 ? "" : "s"} match`;
}

function renderAttributeChips(attributes) {
    const entries = Object.entries(attributes || {});
    if (entries.length === 0) {
        return <span style={{ color: "var(--faint)" }}>No custom attributes</span>;
    }

    return (
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
            {entries.map(([key, value]) => (
                <span className="tag-chip" key={key}>
                    {key}={String(value)}
                </span>
            ))}
        </div>
    );
}

function createAttributeRows(attributes) {
    const entries = Object.entries(attributes || {});
    if (entries.length === 0) {
        return [{ ...DEFAULT_ATTRIBUTE_ROW }];
    }

    return entries.map(([key, value]) => ({
        key,
        value: String(value),
    }));
}

function buildAttributesObject(attributeRows) {
    return attributeRows.reduce((result, row) => {
        const trimmedKey = row.key.trim();
        if (!trimmedKey) return result;
        result[trimmedKey] = coerceAttributeValue(row.value);
        return result;
    }, {});
}

function coerceAttributeValue(rawValue) {
    const value = String(rawValue || "").trim();
    if (value.toLowerCase() === "true") return true;
    if (value.toLowerCase() === "false") return false;
    if (/^-?\d+(\.\d+)?$/.test(value)) return Number(value);
    return value;
}
