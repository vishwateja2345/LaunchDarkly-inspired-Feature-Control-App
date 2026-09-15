export function Spinner({ label = "Loading" }) {
    return (
        <div className="spinner-wrap" role="status">
            <span className="spinner" />
            <span className="sr-only">{label}</span>
        </div>
    );
}
