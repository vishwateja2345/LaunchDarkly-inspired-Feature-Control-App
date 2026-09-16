import { useCallback, useEffect, useMemo, useState } from "react";
import { useWorkspace } from "../../shared/WorkspaceContext.jsx";
import { useRouter } from "../../shared/utils/router.jsx";
import { useToast } from "../../shared/components/Toast.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { EmptyState } from "../../shared/components/EmptyState.jsx";
import { Spinner } from "../../shared/components/Spinner.jsx";
import { Switch } from "../../shared/components/Switch.jsx";
import { ConfirmationDialog } from "../../shared/components/ConfirmationDialog.jsx";
import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { flagsApi } from "./flags.api.js";
import { FlagFormDialog } from "./FlagFormDialog.jsx";

export function FlagsListPage() {
    const { project, environment } = useWorkspace();
    const { navigate } = useRouter();
    const { show } = useToast();

    const [flags, setFlags] = useState([]);
    const [configsByFlagId, setConfigsByFlagId] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [search, setSearch] = useState("");
    const [tagFilter, setTagFilter] = useState("all");
    const [showArchived, setShowArchived] = useState(false);
    const [creating, setCreating] = useState(false);
    const [archiveTarget, setArchiveTarget] = useState(null);
    const [archiveBusy, setArchiveBusy] = useState(false);

    const load = useCallback(async () => {
        if (!project) return;
        setLoading(true);
        setError("");
        try {
            const list = await flagsApi.list(project._id, { includeArchived: showArchived });
            setFlags(list);
            if (environment) {
                const entries = await Promise.all(
                    list
                        .filter((flag) => !flag.archived)
                        .map(async (flag) => {
                            try {
                                const config = await flagsApi.getConfig(flag._id, environment.key);
                                return [flag._id, config];
                            } catch {
                                return [flag._id, null];
                            }
                        }),
                );
                setConfigsByFlagId(Object.fromEntries(entries));
            }
        } catch (caught) {
            setError(caught.message || "Unable to load flags.");
        } finally {
            setLoading(false);
        }
    }, [project, environment, showArchived]);

    useEffect(() => {
        load();
    }, [load]);

    const tags = useMemo(() => {
        const all = new Set();
        flags.forEach((flag) => flag.tags.forEach((tag) => all.add(tag)));
        return ["all", ...Array.from(all).sort()];
    }, [flags]);

    const filteredFlags = useMemo(() => {
        const term = search.trim().toLowerCase();
        return flags.filter((flag) => {
            if (tagFilter !== "all" && !flag.tags.includes(tagFilter)) return false;
            if (!term) return true;
            return (
                flag.name.toLowerCase().includes(term) ||
                flag.key.toLowerCase().includes(term) ||
                flag.description.toLowerCase().includes(term)
            );
        });
    }, [flags, search, tagFilter]);

    const toggleFlag = async (flag, nextEnabled) => {
        if (!environment) return;
        setConfigsByFlagId((current) => ({
            ...current,
            [flag._id]: { ...current[flag._id], enabled: nextEnabled },
        }));
        try {
            const result = await flagsApi.toggle(flag._id, environment.key, nextEnabled);
            if (result.applied) {
                setConfigsByFlagId((current) => ({ ...current, [flag._id]: result.config }));
                show(`${flag.name} is now ${nextEnabled ? "on" : "off"} in ${environment.name}.`, { tone: "success" });
            } else {
                setConfigsByFlagId((current) => ({
                    ...current,
                    [flag._id]: { ...current[flag._id], enabled: !nextEnabled },
                }));
                show(`Submitted for approval - ${environment.name} requires review before this change goes live.`, {
                    tone: "info",
                });
            }
        } catch (caught) {
            setConfigsByFlagId((current) => ({
                ...current,
                [flag._id]: { ...current[flag._id], enabled: !nextEnabled },
            }));
            show(caught.message || "Unable to update this flag.", { tone: "error" });
        }
    };

    const confirmArchive = async () => {
        if (!archiveTarget) return;
        setArchiveBusy(true);
        try {
            if (archiveTarget.archived) {
                await flagsApi.restore(archiveTarget._id);
                show(`${archiveTarget.name} restored.`, { tone: "success" });
            } else {
                await flagsApi.archive(archiveTarget._id);
                show(`${archiveTarget.name} archived.`, { tone: "success" });
            }
            setArchiveTarget(null);
            load();
        } catch (caught) {
            show(caught.message || "Unable to update this flag.", { tone: "error" });
        } finally {
            setArchiveBusy(false);
        }
    };

    if (!project) return null;

    return (
        <div>
            <div className="page-header">
                <div>
                    <h2>Feature flags</h2>
                    <p>Create, target, and roll out flags for {project.name}.</p>
                </div>
                <button className="primary-button" type="button" onClick={() => setCreating(true)}>
                    <MaterialIcon size={18}>add</MaterialIcon> New flag
                </button>
            </div>

            <div className="page-toolbar">
                <div className="search-input">
                    <MaterialIcon size={18}>search</MaterialIcon>
                    <input
                        aria-label="Search flags"
                        placeholder="Search flags by name, key, or description"
                        type="search"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                    />
                </div>
                {tags.length > 1 && (
                    <SelectMenu
                        ariaLabel="Filter by tag"
                        value={tagFilter}
                        onChange={setTagFilter}
                        options={tags.map((tag) => ({ value: tag, label: tag === "all" ? "All tags" : tag }))}
                    />
                )}
                <label className="checkbox-row">
                    <input
                        type="checkbox"
                        checked={showArchived}
                        onChange={(event) => setShowArchived(event.target.checked)}
                    />
                    Show archived
                </label>
            </div>

            {loading && <Spinner label="Loading flags" />}
            {!loading && error && <p className="inline-error">{error}</p>}
            {!loading && !error && filteredFlags.length === 0 && (
                <EmptyState
                    icon="flag"
                    title={flags.length === 0 ? "No flags yet" : "No flags match your filters"}
                    description={
                        flags.length === 0
                            ? "Create your first feature flag to start controlling runtime behavior without a deploy."
                            : "Try a different search term or tag."
                    }
                    action={
                        flags.length === 0 && (
                            <button className="primary-button" type="button" onClick={() => setCreating(true)}>
                                New flag
                            </button>
                        )
                    }
                />
            )}

            {!loading && !error && filteredFlags.length > 0 && (
                <div className="card-list">
                    {filteredFlags.map((flag) => {
                        const config = configsByFlagId[flag._id];
                        return (
                            <div
                                className="flag-row"
                                key={flag._id}
                                role="button"
                                tabIndex={0}
                                onClick={() => navigate(`/p/${project.key}/flags/${flag.key}`)}
                                onKeyDown={(event) => {
                                    if (event.key === "Enter") navigate(`/p/${project.key}/flags/${flag.key}`);
                                }}
                            >
                                <MaterialIcon size={22} className="flag-row-icon">
                                    flag
                                </MaterialIcon>
                                <div className="flag-row-main">
                                    <span className="flag-name">{flag.name}</span>
                                    <span className="flag-key">{flag.key}</span>
                                    {flag.description && <span className="flag-description">{flag.description}</span>}
                                </div>
                                <div className="flag-row-tags">
                                    {flag.archived && <Badge tone="neutral">Archived</Badge>}
                                    {flag.temporary && !flag.archived && <Badge tone="info">Temporary</Badge>}
                                    {flag.tags.map((tag) => (
                                        <span className="tag-chip" key={tag}>
                                            {tag}
                                        </span>
                                    ))}
                                </div>
                                <span className="flag-row-type">
                                    {flag.flagType === "boolean" ? "Boolean" : `${flag.variations.length} variations`}
                                </span>
                                <div className="flag-row-actions" onClick={(event) => event.stopPropagation()}>
                                    {!flag.archived && config && environment && (
                                        <Switch
                                            ariaLabel={`Toggle ${flag.name} in ${environment.name}`}
                                            checked={Boolean(config.enabled)}
                                            onChange={(next) => toggleFlag(flag, next)}
                                        />
                                    )}
                                    <button
                                        className="icon-button"
                                        type="button"
                                        aria-label={flag.archived ? "Restore flag" : "Archive flag"}
                                        onClick={() => setArchiveTarget(flag)}
                                    >
                                        <MaterialIcon size={18}>{flag.archived ? "history" : "delete"}</MaterialIcon>
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            {creating && (
                <FlagFormDialog
                    projectId={project._id}
                    onClose={() => setCreating(false)}
                    onCreated={() => {
                        setCreating(false);
                        load();
                    }}
                />
            )}

            {archiveTarget && (
                <ConfirmationDialog
                    title={archiveTarget.archived ? "Restore this flag?" : "Archive this flag?"}
                    message={
                        archiveTarget.archived
                            ? `${archiveTarget.name} will show up in the active flags list again.`
                            : `${archiveTarget.name} will be hidden from the active list. Its configuration and history are kept.`
                    }
                    confirmLabel={archiveTarget.archived ? "Restore" : "Archive"}
                    danger={!archiveTarget.archived}
                    busy={archiveBusy}
                    onCancel={() => setArchiveTarget(null)}
                    onConfirm={confirmArchive}
                />
            )}
        </div>
    );
}
