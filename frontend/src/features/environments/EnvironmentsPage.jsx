import { useEffect, useId, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { environmentsApi } from "./environments.api.js";
import { useWorkspace } from "../../shared/WorkspaceContext.jsx";
import { useToast } from "../../shared/components/Toast.jsx";
import { Modal } from "../../shared/components/Modal.jsx";
import { ConfirmationDialog } from "../../shared/components/ConfirmationDialog.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { EmptyState } from "../../shared/components/EmptyState.jsx";
import { Spinner } from "../../shared/components/Spinner.jsx";

const DEFAULT_COLOR = "#6366f1";
const INITIAL_FORM_STATE = {
    busy: false,
    error: "",
    fieldErrors: {},
};

export function EnvironmentsPage() {
    const { project, reloadEnvironments } = useWorkspace();
    const { show } = useToast();
    const [environments, setEnvironments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [loadVersion, setLoadVersion] = useState(0);
    const [createOpen, setCreateOpen] = useState(false);
    const [editingEnvironment, setEditingEnvironment] = useState(null);
    const [deletingEnvironment, setDeletingEnvironment] = useState(null);
    const [formState, setFormState] = useState(INITIAL_FORM_STATE);
    const [deleteState, setDeleteState] = useState({ busy: false, error: "" });

    useEffect(() => {
        let active = true;

        const loadEnvironments = async () => {
            if (!project?._id) {
                if (!active) return;
                setEnvironments([]);
                setLoading(false);
                setLoadError("");
                return;
            }

            if (active) {
                setLoading(true);
                setLoadError("");
            }

            try {
                const data = await environmentsApi.list(project._id);
                if (active) setEnvironments(Array.isArray(data) ? data : []);
            } catch (caught) {
                if (active) {
                    setEnvironments([]);
                    setLoadError(caught.message || "Unable to load environments.");
                }
            } finally {
                if (active) setLoading(false);
            }
        };

        setCreateOpen(false);
        setEditingEnvironment(null);
        setDeletingEnvironment(null);
        setFormState(INITIAL_FORM_STATE);
        setDeleteState({ busy: false, error: "" });
        loadEnvironments();

        return () => {
            active = false;
        };
    }, [project?._id, loadVersion]);

    const sortedEnvironments = useMemo(
        () =>
            [...environments].sort(
                (left, right) =>
                    (left.sortOrder ?? 0) - (right.sortOrder ?? 0) ||
                    left.name.localeCompare(right.name),
            ),
        [environments],
    );

    const refreshEnvironments = async () => {
        const data = await reloadEnvironments();
        setEnvironments(Array.isArray(data) ? data : []);
        return data;
    };

    const openCreateDialog = () => {
        setFormState(INITIAL_FORM_STATE);
        setCreateOpen(true);
    };

    const openEditDialog = (environment) => {
        setFormState(INITIAL_FORM_STATE);
        setEditingEnvironment(environment);
    };

    const openDeleteDialog = (environment) => {
        setDeleteState({ busy: false, error: "" });
        setDeletingEnvironment(environment);
    };

    const closeFormDialog = () => {
        if (formState.busy) return;
        setCreateOpen(false);
        setEditingEnvironment(null);
        setFormState(INITIAL_FORM_STATE);
    };

    const closeDeleteDialog = () => {
        if (deleteState.busy) return;
        setDeletingEnvironment(null);
        setDeleteState({ busy: false, error: "" });
    };

    const createEnvironment = async (input) => {
        setFormState({ busy: true, error: "", fieldErrors: {} });

        try {
            await environmentsApi.create(project._id, input);
            await refreshEnvironments();
            setCreateOpen(false);
            setFormState(INITIAL_FORM_STATE);
            show(`Created ${input.name}.`, { tone: "success" });
        } catch (caught) {
            setFormState({
                busy: false,
                error: caught.message || "Unable to create environment.",
                fieldErrors: caught.details?.fieldErrors || {},
            });
        }
    };

    const updateEnvironment = async (input) => {
        if (!editingEnvironment) return;

        setFormState({ busy: true, error: "", fieldErrors: {} });

        try {
            await environmentsApi.update(editingEnvironment._id, input);
            await refreshEnvironments();
            setEditingEnvironment(null);
            setFormState(INITIAL_FORM_STATE);
            show(`Updated ${input.name}.`, { tone: "success" });
        } catch (caught) {
            setFormState({
                busy: false,
                error: caught.message || "Unable to update environment.",
                fieldErrors: caught.details?.fieldErrors || {},
            });
        }
    };

    const deleteEnvironment = async () => {
        if (!deletingEnvironment) return;

        setDeleteState({ busy: true, error: "" });

        try {
            await environmentsApi.remove(deletingEnvironment._id);
            await refreshEnvironments();
            const deletedName = deletingEnvironment.name;
            setDeletingEnvironment(null);
            setDeleteState({ busy: false, error: "" });
            show(`Deleted ${deletedName}.`, { tone: "success" });
        } catch (caught) {
            const message = caught.message || "Unable to delete environment.";
            setDeleteState({ busy: false, error: message });
            show(message, { tone: "error" });
        }
    };

    if (!project) {
        return <Spinner label="Loading environments" />;
    }

    return (
        <>
            <section className="page-header">
                <div>
                    <h2>Project environments</h2>
                    <p>
                        Manage the delivery stages for <strong>{project.name}</strong>, including
                        production safeguards and the shared environment switcher.
                    </p>
                </div>
                <button className="primary-button" type="button" onClick={openCreateDialog}>
                    New environment
                </button>
            </section>

            {loading ? (
                <section className="card">
                    <Spinner label="Loading environments" />
                </section>
            ) : loadError ? (
                <section className="card" style={{ display: "grid", gap: 12 }}>
                    <p className="inline-error" role="alert">
                        {loadError}
                    </p>
                    <div>
                        <button
                            className="secondary-button"
                            type="button"
                            onClick={() => setLoadVersion((current) => current + 1)}
                        >
                            Retry
                        </button>
                    </div>
                </section>
            ) : sortedEnvironments.length === 0 ? (
                <section className="card">
                    <EmptyState
                        icon="layers"
                        title="No environments yet"
                        description="Create Development, Staging, or Production environments so your team can manage flag behavior safely."
                        action={
                            <button className="primary-button" type="button" onClick={openCreateDialog}>
                                New environment
                            </button>
                        }
                    />
                </section>
            ) : (
                <section className="card">
                    <div style={{ overflowX: "auto" }}>
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th scope="col">Name</th>
                                    <th scope="col">Key</th>
                                    <th scope="col">Color</th>
                                    <th scope="col">Type</th>
                                    <th scope="col">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {sortedEnvironments.map((environment) => (
                                    <tr key={environment._id}>
                                        <td>
                                            <strong>{environment.name}</strong>
                                        </td>
                                        <td>
                                            <code>{environment.key}</code>
                                        </td>
                                        <td>
                                            <div
                                                style={{
                                                    display: "inline-flex",
                                                    alignItems: "center",
                                                    gap: 8,
                                                    minWidth: 120,
                                                }}
                                            >
                                                <span
                                                    aria-hidden="true"
                                                    style={{
                                                        width: 14,
                                                        height: 14,
                                                        borderRadius: "999px",
                                                        background: environment.color || DEFAULT_COLOR,
                                                        boxShadow: "0 0 0 1px var(--border-strong) inset",
                                                        flex: "0 0 auto",
                                                    }}
                                                />
                                                <code>{environment.color || DEFAULT_COLOR}</code>
                                            </div>
                                        </td>
                                        <td>
                                            {environment.production ? (
                                                <Badge tone="danger">Production</Badge>
                                            ) : (
                                                <span style={{ color: "var(--muted)" }}>Standard</span>
                                            )}
                                        </td>
                                        <td>
                                            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                                                <button
                                                    className="secondary-button"
                                                    type="button"
                                                    onClick={() => openEditDialog(environment)}
                                                >
                                                    Edit
                                                </button>
                                                <button
                                                    className="danger-button"
                                                    type="button"
                                                    onClick={() => openDeleteDialog(environment)}
                                                >
                                                    Delete
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            )}

            {createOpen && (
                <EnvironmentFormDialog
                    busy={formState.busy}
                    error={formState.error}
                    fieldErrors={formState.fieldErrors}
                    mode="create"
                    onClose={closeFormDialog}
                    onSubmit={createEnvironment}
                />
            )}

            {editingEnvironment && (
                <EnvironmentFormDialog
                    busy={formState.busy}
                    environment={editingEnvironment}
                    error={formState.error}
                    fieldErrors={formState.fieldErrors}
                    mode="edit"
                    onClose={closeFormDialog}
                    onSubmit={updateEnvironment}
                />
            )}

            {deletingEnvironment && (
                <ConfirmationDialog
                    busy={deleteState.busy}
                    confirmLabel="Delete environment"
                    danger
                    error={deleteState.error}
                    message={
                        deletingEnvironment.production
                            ? "Deleting this production environment removes its approval-gated targeting history from the current project."
                            : "This environment will be removed from the project for everyone on the team."
                    }
                    onCancel={closeDeleteDialog}
                    onConfirm={deleteEnvironment}
                    title={`Delete ${deletingEnvironment.name}?`}
                />
            )}
        </>
    );
}

function EnvironmentFormDialog({ busy, environment, error, fieldErrors = {}, mode, onClose, onSubmit }) {
    const titleId = `environment-dialog-${useId().replace(/:/g, "")}`;
    const nameId = `environment-name-${useId().replace(/:/g, "")}`;
    const keyId = `environment-key-${useId().replace(/:/g, "")}`;
    const colorId = `environment-color-${useId().replace(/:/g, "")}`;
    const colorTextId = `environment-color-text-${useId().replace(/:/g, "")}`;
    const productionId = `environment-production-${useId().replace(/:/g, "")}`;
    const [name, setName] = useState(environment?.name || "");
    const [key, setKey] = useState(environment?.key || "");
    const [color, setColor] = useState(environment?.color || DEFAULT_COLOR);
    const [production, setProduction] = useState(Boolean(environment?.production));
    const keyError = firstFieldError(fieldErrors, "key");
    const nameError = firstFieldError(fieldErrors, "name");
    const colorError = firstFieldError(fieldErrors, "color");
    const productionError = firstFieldError(fieldErrors, "production");

    const submit = (event) => {
        event.preventDefault();

        if (mode === "create") {
            onSubmit({
                name: name.trim(),
                color: color.trim() || DEFAULT_COLOR,
                production,
                ...(key.trim() ? { key: key.trim() } : {}),
            });
            return;
        }

        onSubmit({
            name: name.trim(),
            color: color.trim() || DEFAULT_COLOR,
        });
    };

    return createPortal(
        <Modal labelledBy={titleId} onClose={() => !busy && onClose()}>
            <form className="form-dialog" onSubmit={submit}>
                <h2 id={titleId}>{mode === "create" ? "New environment" : `Edit ${environment?.name}`}</h2>

                {error && (
                    <p className="inline-error" role="alert" style={{ margin: "0 0 16px" }}>
                        {error}
                    </p>
                )}

                <div className="field-group">
                    <label htmlFor={nameId}>Name</label>
                    <input
                        data-autofocus
                        disabled={busy}
                        id={nameId}
                        required
                        type="text"
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                    />
                    {nameError && (
                        <p className="field-error" role="alert">
                            {nameError}
                        </p>
                    )}
                </div>

                {mode === "create" ? (
                    <>
                        <div className="field-group">
                            <label htmlFor={keyId}>Key</label>
                            <input
                                autoCapitalize="none"
                                disabled={busy}
                                id={keyId}
                                inputMode="text"
                                pattern="[a-z0-9-]*"
                                placeholder="development"
                                spellCheck="false"
                                type="text"
                                value={key}
                                onChange={(event) => setKey(event.target.value.toLowerCase())}
                            />
                            <p className="field-hint">
                                Leave blank to generate a key from the name. Use lowercase letters,
                                numbers, and hyphens only when setting one manually.
                            </p>
                            {keyError && (
                                <p className="field-error" role="alert">
                                    {keyError}
                                </p>
                            )}
                        </div>

                        <div className="field-group">
                            <div className="checkbox-row">
                                <input
                                    checked={production}
                                    disabled={busy}
                                    id={productionId}
                                    type="checkbox"
                                    onChange={(event) => setProduction(event.target.checked)}
                                />
                                <label htmlFor={productionId} style={{ color: "var(--ink)", fontWeight: 500 }}>
                                    Production environment
                                </label>
                            </div>
                            <p className="field-hint" style={{ margin: "0 0 0 28px" }}>
                                Production environment changes will require a second person&apos;s
                                approval before they go live.
                            </p>
                            {productionError && (
                                <p className="field-error" role="alert" style={{ margin: "0 0 0 28px" }}>
                                    {productionError}
                                </p>
                            )}
                        </div>
                    </>
                ) : (
                    <div
                        style={{
                            display: "grid",
                            gap: 10,
                            gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                            marginBottom: 16,
                        }}
                    >
                        <div
                            style={{
                                border: "1px solid var(--border)",
                                borderRadius: 10,
                                padding: 12,
                                background: "var(--surface-muted)",
                            }}
                        >
                            <div style={{ color: "var(--muted)", fontSize: 12, marginBottom: 6 }}>Key</div>
                            <code>{environment?.key}</code>
                        </div>
                        <div
                            style={{
                                border: "1px solid var(--border)",
                                borderRadius: 10,
                                padding: 12,
                                background: "var(--surface-muted)",
                            }}
                        >
                            <div style={{ color: "var(--muted)", fontSize: 12, marginBottom: 6 }}>Type</div>
                            {environment?.production ? (
                                <Badge tone="danger">Production</Badge>
                            ) : (
                                <span style={{ color: "var(--muted)" }}>Standard</span>
                            )}
                        </div>
                    </div>
                )}

                <div className="field-group">
                    <label htmlFor={colorId}>Color</label>
                    <div className="field-row">
                        <input
                            aria-label="Environment color picker"
                            disabled={busy}
                            id={colorId}
                            style={{ minHeight: 42, padding: 4 }}
                            type="color"
                            value={isHexColor(color) ? color : DEFAULT_COLOR}
                            onChange={(event) => setColor(event.target.value)}
                        />
                        <input
                            aria-label="Environment color value"
                            disabled={busy}
                            id={colorTextId}
                            placeholder="#6366f1"
                            spellCheck="false"
                            type="text"
                            value={color}
                            onChange={(event) => setColor(event.target.value)}
                        />
                    </div>
                    <p className="field-hint">
                        Choose a recognizable swatch for the environment selector and management
                        views.
                    </p>
                    {colorError && (
                        <p className="field-error" role="alert">
                            {colorError}
                        </p>
                    )}
                </div>

                <footer>
                    <button className="secondary-button" disabled={busy} type="button" onClick={onClose}>
                        Cancel
                    </button>
                    <button className="primary-button" disabled={busy} type="submit">
                        {busy
                            ? mode === "create"
                                ? "Creating…"
                                : "Saving…"
                            : mode === "create"
                              ? "Create environment"
                              : "Save changes"}
                    </button>
                </footer>
            </form>
        </Modal>,
        document.body,
    );
}

function firstFieldError(fieldErrors, fieldName) {
    const messages = fieldErrors?.[fieldName];
    return Array.isArray(messages) && messages.length > 0 ? messages[0] : "";
}

function isHexColor(value) {
    return /^#[0-9a-f]{6}$/i.test(value || "");
}
