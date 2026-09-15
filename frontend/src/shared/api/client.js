const baseUrl = import.meta.env.VITE_API_URL || "/api/v1";
let sessionToken = localStorage.getItem("flagdeck-session-token") || "";

export function setSessionToken(token) {
    sessionToken = token || "";
    if (sessionToken) localStorage.setItem("flagdeck-session-token", sessionToken);
    else localStorage.removeItem("flagdeck-session-token");
}

export const hasSessionToken = () => Boolean(sessionToken);

export async function request(path, options = {}) {
    const response = await fetch(`${baseUrl}${path}`, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...(sessionToken ? { Authorization: `Bearer ${sessionToken}` } : {}),
            ...options.headers,
        },
    });

    if (response.status === 204) return null;

    const contentType = response.headers?.get?.("content-type") || "";
    const payload = contentType.includes("application/json") || !response.headers
        ? await response.json().catch(() => null)
        : null;

    if (!response.ok) {
        const code = payload?.error?.code;
        const message = payload?.error?.message || `FlagDeck service returned ${response.status}.`;
        const details = payload?.error?.details;

        if (response.status === 401 && ["AUTH_REQUIRED", "INVALID_TOKEN", "ACCOUNT_UNAVAILABLE"].includes(code)) {
            setSessionToken("");
            window.dispatchEvent(new CustomEvent("flagdeck-session-expired", { detail: message }));
        }

        const error = new Error(message);
        error.code = code;
        error.details = details;
        error.status = response.status;
        throw error;
    }

    if (!payload || !("data" in payload)) throw new Error("FlagDeck service returned an invalid response.");
    return payload.data;
}

export const api = {
    get: (path) => request(path),
    post: (path, body) => request(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) }),
    patch: (path, body) => request(path, { method: "PATCH", body: JSON.stringify(body) }),
    put: (path, body) => request(path, { method: "PUT", body: JSON.stringify(body) }),
    delete: (path) => request(path, { method: "DELETE" }),
};
