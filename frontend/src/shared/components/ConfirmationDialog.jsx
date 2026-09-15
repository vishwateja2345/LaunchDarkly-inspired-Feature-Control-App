import { createPortal } from "react-dom";
import { useId } from "react";
import { Modal } from "./Modal.jsx";

export function ConfirmationDialog({
    busy = false,
    confirmLabel = "Continue",
    danger = false,
    error = "",
    message = "",
    onCancel,
    onConfirm,
    title,
}) {
    const titleId = `confirmation-${useId().replace(/:/g, "")}`;
    return createPortal(
        <Modal className="confirmation-dialog-modal" labelledBy={titleId} onClose={() => !busy && onCancel()}>
            <section className="confirmation-dialog">
                <h2 id={titleId}>{title}</h2>
                {message && <p>{message}</p>}
                {error && (
                    <p className="confirmation-dialog-error" role="alert">
                        {error}
                    </p>
                )}
                <footer>
                    <button data-autofocus disabled={busy} type="button" onClick={onCancel}>
                        Cancel
                    </button>
                    <button
                        className={danger ? "confirmation-confirm confirmation-confirm-danger" : "confirmation-confirm"}
                        disabled={busy}
                        type="button"
                        onClick={onConfirm}
                    >
                        {busy ? "Working…" : confirmLabel}
                    </button>
                </footer>
            </section>
        </Modal>,
        document.body,
    );
}
