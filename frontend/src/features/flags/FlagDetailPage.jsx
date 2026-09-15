import { useCallback, useEffect, useState } from "react";
import { useWorkspace } from "../../shared/WorkspaceContext.jsx";
import { useRouter } from "../../shared/utils/router.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { Badge } from "../../shared/components/Badge.jsx";
import { Spinner } from "../../shared/components/Spinner.jsx";
import { flagsApi } from "./flags.api.js";
import { TargetingRolloutEditor } from "../targeting/TargetingRolloutEditor.jsx";
import { ExperimentPanel } from "../experiments/ExperimentPanel.jsx";
import { FlagHistoryPanel } from "../history/FlagHistoryPanel.jsx";

const TABS = [
    { key: "targeting", label: "Targeting & rollout" },
    { key: "experiment", label: "Experiment" },
    { key: "history", label: "History" },
];

export function FlagDetailPage({ flagKey }) {
    const { project, environment } = useWorkspace();
    const { navigate } = useRouter();
    const [flag, setFlag] = useState(null);
    const [config, setConfig] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [tab, setTab] = useState("targeting");

    const load = useCallback(async () => {
        if (!project || !environment) return;
        setLoading(true);
        setError("");
        try {
            const flags = await flagsApi.list(project._id, { includeArchived: true });
            const found = flags.find((candidate) => candidate.key === flagKey);
            if (!found) {
                setError("This flag could not be found.");
                setFlag(null);
                return;
            }
            setFlag(found);
            const flagConfig = await flagsApi.getConfig(found._id, environment.key);
            setConfig(flagConfig);
        } catch (caught) {
            setError(caught.message || "Unable to load this flag.");
        } finally {
            setLoading(false);
        }
    }, [project, environment, flagKey]);

    useEffect(() => {
        load();
    }, [load]);

    if (loading) return <Spinner label="Loading flag" />;
    if (error) return <p className="inline-error">{error}</p>;
    if (!flag || !config || !environment) return null;

    return (
        <div>
            <button className="link-button" type="button" onClick={() => navigate(`/p/${project.key}/flags`)}>
                <MaterialIcon size={16}>arrow_back</MaterialIcon> All flags
            </button>

            <div className="page-header" style={{ marginTop: 10 }}>
                <div>
                    <h2 style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        {flag.name}
                        {flag.archived && <Badge tone="neutral">Archived</Badge>}
                    </h2>
                    <p>
                        <code>{flag.key}</code> · {flag.description || "No description"}
                    </p>
                </div>
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                    {flag.tags.map((tag) => (
                        <span className="tag-chip" key={tag}>
                            {tag}
                        </span>
                    ))}
                </div>
            </div>

            <div className="tab-bar" role="tablist">
                {TABS.map((item) => (
                    <button
                        key={item.key}
                        role="tab"
                        aria-selected={tab === item.key}
                        className={tab === item.key ? "tab-item tab-item-active" : "tab-item"}
                        type="button"
                        onClick={() => setTab(item.key)}
                    >
                        {item.label}
                    </button>
                ))}
            </div>

            {tab === "targeting" && (
                <TargetingRolloutEditor
                    config={config}
                    environment={environment}
                    flag={flag}
                    projectId={project._id}
                    onSaved={load}
                />
            )}
            {tab === "experiment" && <ExperimentPanel flag={flag} environmentKey={environment.key} />}
            {tab === "history" && (
                <FlagHistoryPanel flagId={flag._id} environment={environment} onRestored={load} />
            )}
        </div>
    );
}
