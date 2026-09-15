import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { authApi } from "../features/auth/auth.api.js";
import { hasSessionToken, setSessionToken } from "./api/client.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [account, setAccount] = useState(null);
    const [status, setStatus] = useState("loading"); // loading | signed-out | signed-in

    const loadSession = useCallback(async () => {
        if (!hasSessionToken()) {
            setStatus("signed-out");
            return;
        }
        try {
            const data = await authApi.session();
            setAccount(data.account);
            setStatus("signed-in");
        } catch {
            setSessionToken("");
            setStatus("signed-out");
        }
    }, []);

    useEffect(() => {
        loadSession();
    }, [loadSession]);

    useEffect(() => {
        const onExpired = () => {
            setAccount(null);
            setStatus("signed-out");
        };
        window.addEventListener("flagdeck-session-expired", onExpired);
        return () => window.removeEventListener("flagdeck-session-expired", onExpired);
    }, []);

    const login = useCallback(async (email, password) => {
        const data = await authApi.login(email, password);
        setSessionToken(data.token);
        setAccount(data.account);
        setStatus("signed-in");
    }, []);

    const logout = useCallback(async () => {
        try {
            await authApi.logout();
        } catch {
            // Token is discarded client-side regardless of network outcome.
        }
        setSessionToken("");
        setAccount(null);
        setStatus("signed-out");
    }, []);

    return (
        <AuthContext.Provider value={{ account, status, login, logout }}>{children}</AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) throw new Error("useAuth must be used within an AuthProvider");
    return context;
}
