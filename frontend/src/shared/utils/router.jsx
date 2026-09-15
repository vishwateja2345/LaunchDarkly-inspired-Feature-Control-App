import { createContext, useCallback, useContext, useEffect, useState } from "react";

const RouterContext = createContext(null);

export function RouterProvider({ children }) {
    const [path, setPath] = useState(() => window.location.pathname);

    useEffect(() => {
        const onPopState = () => setPath(window.location.pathname);
        window.addEventListener("popstate", onPopState);
        return () => window.removeEventListener("popstate", onPopState);
    }, []);

    const navigate = useCallback((to, { replace = false } = {}) => {
        if (to === window.location.pathname) return;
        if (replace) window.history.replaceState({}, "", to);
        else window.history.pushState({}, "", to);
        setPath(to);
    }, []);

    return <RouterContext.Provider value={{ path, navigate }}>{children}</RouterContext.Provider>;
}

export function useRouter() {
    const context = useContext(RouterContext);
    if (!context) throw new Error("useRouter must be used within a RouterProvider");
    return context;
}

/** Matches a `/p/:projectKey/segment/:id` style pattern against the current path. */
export function matchPath(pattern, path) {
    const patternParts = pattern.split("/").filter(Boolean);
    const pathParts = path.split("/").filter(Boolean);
    if (patternParts.length !== pathParts.length) return null;

    const params = {};
    for (let index = 0; index < patternParts.length; index += 1) {
        const patternPart = patternParts[index];
        const pathPart = pathParts[index];
        if (patternPart.startsWith(":")) {
            params[patternPart.slice(1)] = decodeURIComponent(pathPart);
        } else if (patternPart !== pathPart) {
            return null;
        }
    }
    return params;
}
