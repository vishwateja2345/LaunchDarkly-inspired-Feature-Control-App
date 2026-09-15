const TONE_CLASS = {
    neutral: "badge-neutral",
    success: "badge-success",
    warning: "badge-warning",
    danger: "badge-danger",
    info: "badge-info",
};

export function Badge({ children, tone = "neutral" }) {
    return <span className={`badge ${TONE_CLASS[tone] || TONE_CLASS.neutral}`}>{children}</span>;
}
