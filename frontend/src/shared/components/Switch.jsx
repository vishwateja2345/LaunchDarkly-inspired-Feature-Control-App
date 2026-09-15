export function Switch({ ariaLabel, checked, disabled = false, onChange }) {
    return (
        <button
            aria-checked={checked}
            aria-label={ariaLabel}
            className={`switch ${checked ? "switch-on" : ""}`}
            disabled={disabled}
            role="switch"
            type="button"
            onClick={() => onChange(!checked)}
        >
            <span className="switch-thumb" />
        </button>
    );
}
