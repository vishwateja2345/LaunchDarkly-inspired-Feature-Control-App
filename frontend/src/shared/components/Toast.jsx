import { createContext, useCallback, useContext, useRef, useState } from "react";
import { MaterialIcon } from "./MaterialIcon.jsx";

const ToastContext = createContext(null);

let nextId = 1;

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);
    const timers = useRef(new Map());

    const dismiss = useCallback((id) => {
        setToasts((current) => current.filter((toast) => toast.id !== id));
        clearTimeout(timers.current.get(id));
        timers.current.delete(id);
    }, []);

    const show = useCallback(
        (message, { tone = "info", duration = 5000 } = {}) => {
            const id = nextId++;
            setToasts((current) => [...current, { id, message, tone }]);
            timers.current.set(
                id,
                setTimeout(() => dismiss(id), duration),
            );
            return id;
        },
        [dismiss],
    );

    return (
        <ToastContext.Provider value={{ show, dismiss }}>
            {children}
            <div className="toast-stack" role="status" aria-live="polite">
                {toasts.map((toast) => (
                    <div className={`toast toast-${toast.tone}`} key={toast.id}>
                        <MaterialIcon size={18}>
                            {toast.tone === "success" ? "check_circle" : toast.tone === "error" ? "error" : "info"}
                        </MaterialIcon>
                        <span>{toast.message}</span>
                        <button aria-label="Dismiss notification" type="button" onClick={() => dismiss(toast.id)}>
                            <MaterialIcon size={16}>close</MaterialIcon>
                        </button>
                    </div>
                ))}
            </div>
        </ToastContext.Provider>
    );
}

export function useToast() {
    const context = useContext(ToastContext);
    if (!context) throw new Error("useToast must be used within a ToastProvider");
    return context;
}
