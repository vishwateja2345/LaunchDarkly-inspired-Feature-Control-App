import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { projectsApi } from "../features/projects/projects.api.js";
import { environmentsApi } from "../features/environments/environments.api.js";

const WorkspaceContext = createContext(null);

const PROJECT_KEY_STORAGE = "flagdeck-project-key";
const environmentStorageKey = (projectId) => `flagdeck-environment-${projectId}`;

export function WorkspaceProvider({ children }) {
    const [projects, setProjects] = useState([]);
    const [projectsLoaded, setProjectsLoaded] = useState(false);
    const [selectedProjectKey, setSelectedProjectKey] = useState(() => localStorage.getItem(PROJECT_KEY_STORAGE));
    const [environments, setEnvironments] = useState([]);
    const [selectedEnvironmentKey, setSelectedEnvironmentKey] = useState(null);

    const reloadProjects = useCallback(async () => {
        const data = await projectsApi.list();
        setProjects(data);
        setProjectsLoaded(true);
        return data;
    }, []);

    useEffect(() => {
        reloadProjects();
    }, [reloadProjects]);

    useEffect(() => {
        if (!projectsLoaded) return;
        if (projects.length === 0) return;
        const stillExists = projects.some((project) => project.key === selectedProjectKey);
        if (!stillExists) {
            setSelectedProjectKey(projects[0].key);
        }
    }, [projects, projectsLoaded, selectedProjectKey]);

    const project = useMemo(
        // Falls back to the first project synchronously so there is never a render where
        // projects exist but none is "selected" yet (the effect below only persists the
        // default choice - it must not be the only thing that resolves it).
        () => projects.find((candidate) => candidate.key === selectedProjectKey) || projects[0] || null,
        [projects, selectedProjectKey],
    );

    const selectProject = useCallback((key) => {
        localStorage.setItem(PROJECT_KEY_STORAGE, key);
        setSelectedProjectKey(key);
        setEnvironments([]);
        setSelectedEnvironmentKey(null);
    }, []);

    const reloadEnvironments = useCallback(async () => {
        if (!project) return [];
        const data = await environmentsApi.list(project._id);
        setEnvironments(data);
        return data;
    }, [project]);

    useEffect(() => {
        if (!project) return;
        reloadEnvironments();
    }, [project, reloadEnvironments]);

    useEffect(() => {
        if (!project || environments.length === 0) return;
        const stored = localStorage.getItem(environmentStorageKey(project._id));
        const stillExists = environments.some((environment) => environment.key === stored);
        if (stillExists) {
            setSelectedEnvironmentKey(stored);
        } else {
            setSelectedEnvironmentKey(environments[0].key);
        }
    }, [project, environments]);

    const selectEnvironment = useCallback(
        (key) => {
            if (project) localStorage.setItem(environmentStorageKey(project._id), key);
            setSelectedEnvironmentKey(key);
        },
        [project],
    );

    const environment = useMemo(
        () => environments.find((candidate) => candidate.key === selectedEnvironmentKey) || null,
        [environments, selectedEnvironmentKey],
    );

    const value = useMemo(
        () => ({
            projects,
            projectsLoaded,
            project,
            selectProject,
            reloadProjects,
            environments,
            environment,
            selectEnvironment,
            reloadEnvironments,
        }),
        [projects, projectsLoaded, project, selectProject, reloadProjects, environments, environment, selectEnvironment, reloadEnvironments],
    );

    return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}

export function useWorkspace() {
    const context = useContext(WorkspaceContext);
    if (!context) throw new Error("useWorkspace must be used within a WorkspaceProvider");
    return context;
}
