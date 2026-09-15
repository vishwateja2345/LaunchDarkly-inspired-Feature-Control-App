import { useEffect, useMemo, useState } from "react";
import { AuthProvider, useAuth } from "./shared/AuthContext.jsx";
import { WorkspaceProvider, useWorkspace } from "./shared/WorkspaceContext.jsx";
import { RouterProvider, matchPath, useRouter } from "./shared/utils/router.jsx";
import { ToastProvider } from "./shared/components/Toast.jsx";
import { MaterialIcon } from "./shared/components/MaterialIcon.jsx";
import { SelectMenu } from "./shared/components/SelectMenu.jsx";
import { Spinner } from "./shared/components/Spinner.jsx";
import { applyTheme, readTheme } from "./shared/utils/theme.js";
import { identityColor, identityInitials } from "./shared/utils/identity.js";
import { LoginPage } from "./features/auth/LoginPage.jsx";
import { FlagsListPage } from "./features/flags/FlagsListPage.jsx";
import { FlagDetailPage } from "./features/flags/FlagDetailPage.jsx";
import { EnvironmentsPage } from "./features/environments/EnvironmentsPage.jsx";
import { SegmentsPage } from "./features/targeting/SegmentsPage.jsx";
import { ApprovalsPage } from "./features/approvals/ApprovalsPage.jsx";
import { ProjectHistoryPage } from "./features/history/ProjectHistoryPage.jsx";

const NAV_ITEMS = [
    { icon: "flag", key: "flags", label: "Flags" },
    { icon: "layers", key: "environments", label: "Environments" },
    { icon: "group", key: "segments", label: "Targeting" },
    { icon: "approval", key: "approvals", label: "Approvals" },
    { icon: "history", key: "history", label: "History" },
];

export default function App() {
    return (
        <ToastProvider>
            <RouterProvider>
                <AuthProvider>
                    <Gate />
                </AuthProvider>
            </RouterProvider>
        </ToastProvider>
    );
}

function Gate() {
    const { status } = useAuth();

    if (status === "loading") {
        return (
            <div className="app-boot">
                <div className="app-boot-brand">
                    <MaterialIcon size={28}>flag</MaterialIcon>
                    <strong>FlagDeck</strong>
                </div>
                <div className="app-boot-progress" />
            </div>
        );
    }

    if (status === "signed-out") {
        return <LoginPage />;
    }

    return (
        <WorkspaceProvider>
            <Shell />
        </WorkspaceProvider>
    );
}

function Shell() {
    const { account, logout } = useAuth();
    const { path, navigate } = useRouter();
    const { projects, projectsLoaded, project, selectProject, environments, environment, selectEnvironment } =
        useWorkspace();
    const [theme, setTheme] = useState(readTheme());

    useEffect(() => {
        applyTheme(theme);
    }, [theme]);

    useEffect(() => {
        if (path === "/" && project) {
            navigate(`/p/${project.key}/flags`, { replace: true });
        }
    }, [path, project, navigate]);

    const activeSection = useMemo(() => {
        for (const item of NAV_ITEMS) {
            if (matchPath(`/p/:projectKey/${item.key}`, path) || matchPath(`/p/:projectKey/${item.key}/:rest`, path)) {
                return item.key;
            }
        }
        return "flags";
    }, [path]);

    if (!projectsLoaded) {
        return (
            <div className="app-boot">
                <Spinner label="Loading workspace" />
            </div>
        );
    }

    if (projects.length === 0) {
        return (
            <div className="app-boot">
                <p>No projects yet. Seed the database or create one from the API to get started.</p>
            </div>
        );
    }

    if (!project) {
        return (
            <div className="app-boot">
                <Spinner label="Loading workspace" />
            </div>
        );
    }

    return (
        <div className="app-shell">
            <aside className="app-sidebar">
                <div className="app-sidebar-brand">
                    <MaterialIcon size={24}>flag</MaterialIcon>
                    <span>FlagDeck</span>
                </div>

                <div className="app-sidebar-project">
                    <span className="app-sidebar-label">Project</span>
                    <SelectMenu
                        ariaLabel="Select project"
                        value={project?.key}
                        onChange={(key) => {
                            selectProject(key);
                            navigate(`/p/${key}/flags`);
                        }}
                        options={projects.map((candidate) => ({ value: candidate.key, label: candidate.name }))}
                    />
                </div>

                <nav className="app-sidebar-nav" aria-label="Primary">
                    {NAV_ITEMS.map((item) => (
                        <button
                            key={item.key}
                            type="button"
                            className={activeSection === item.key ? "app-nav-item app-nav-item-active" : "app-nav-item"}
                            onClick={() => navigate(`/p/${project.key}/${item.key}`)}
                            aria-current={activeSection === item.key ? "page" : undefined}
                        >
                            <MaterialIcon size={20}>{item.icon}</MaterialIcon>
                            <span>{item.label}</span>
                        </button>
                    ))}
                </nav>

                <div className="app-sidebar-footer">
                    <button
                        type="button"
                        className="app-nav-item"
                        onClick={() => setTheme((current) => applyTheme(current === "dark" ? "light" : "dark"))}
                    >
                        <MaterialIcon size={20}>{theme === "dark" ? "light_mode" : "dark_mode"}</MaterialIcon>
                        <span>{theme === "dark" ? "Light theme" : "Dark theme"}</span>
                    </button>
                    <div className="app-account">
                        <span className="avatar" style={{ background: identityColor(account?.name) }}>
                            {identityInitials(account?.name)}
                        </span>
                        <div className="app-account-info">
                            <strong>{account?.name}</strong>
                            <span>{account?.role}</span>
                        </div>
                        <button type="button" aria-label="Sign out" onClick={logout}>
                            <MaterialIcon size={20}>logout</MaterialIcon>
                        </button>
                    </div>
                </div>
            </aside>

            <div className="app-main">
                <header className="app-topbar">
                    <h1>{NAV_ITEMS.find((item) => item.key === activeSection)?.label}</h1>
                    {environments.length > 0 && (
                        <div className="app-environment-switcher" role="tablist" aria-label="Environment">
                            {environments.map((candidateEnvironment) => (
                                <button
                                    key={candidateEnvironment.key}
                                    type="button"
                                    role="tab"
                                    aria-selected={environment?.key === candidateEnvironment.key}
                                    className={
                                        environment?.key === candidateEnvironment.key
                                            ? "environment-pill environment-pill-active"
                                            : "environment-pill"
                                    }
                                    style={{ "--environment-color": candidateEnvironment.color }}
                                    onClick={() => selectEnvironment(candidateEnvironment.key)}
                                >
                                    {candidateEnvironment.production && (
                                        <MaterialIcon size={14}>warning_amber</MaterialIcon>
                                    )}
                                    {candidateEnvironment.name}
                                </button>
                            ))}
                        </div>
                    )}
                </header>
                <main className="app-content">
                    <PageRouter path={path} projectKey={project.key} />
                </main>
            </div>
        </div>
    );
}

function PageRouter({ path, projectKey }) {
    if (matchPath(`/p/${projectKey}/flags`, path)) return <FlagsListPage />;
    const flagMatch = matchPath(`/p/${projectKey}/flags/:flagKey`, path);
    if (flagMatch) return <FlagDetailPage flagKey={flagMatch.flagKey} />;
    if (matchPath(`/p/${projectKey}/environments`, path)) return <EnvironmentsPage />;
    if (matchPath(`/p/${projectKey}/segments`, path)) return <SegmentsPage />;
    if (matchPath(`/p/${projectKey}/approvals`, path)) return <ApprovalsPage />;
    if (matchPath(`/p/${projectKey}/history`, path)) return <ProjectHistoryPage />;
    return <FlagsListPage />;
}
