const RELATIVE = new Intl.RelativeTimeFormat("en", { numeric: "auto" });
const DATE_TIME = new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
});
const DATE_ONLY = new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", year: "numeric" });

export function formatDateTime(value) {
    if (!value) return "—";
    return DATE_TIME.format(new Date(value));
}

export function formatDate(value) {
    if (!value) return "—";
    return DATE_ONLY.format(new Date(value));
}

const DIVISIONS = [
    { amount: 60, unit: "seconds" },
    { amount: 60, unit: "minutes" },
    { amount: 24, unit: "hours" },
    { amount: 7, unit: "days" },
    { amount: 4.34524, unit: "weeks" },
    { amount: 12, unit: "months" },
    { amount: Number.POSITIVE_INFINITY, unit: "years" },
];

export function formatRelativeTime(value) {
    if (!value) return "—";
    let duration = (new Date(value).getTime() - Date.now()) / 1000;

    for (const division of DIVISIONS) {
        if (Math.abs(duration) < division.amount) {
            return RELATIVE.format(Math.round(duration), division.unit);
        }
        duration /= division.amount;
    }

    return RELATIVE.format(Math.round(duration), "years");
}

export function formatPercent(value, digits = 1) {
    if (value === null || value === undefined || Number.isNaN(value)) return "—";
    return `${Number(value).toFixed(digits)}%`;
}

export function formatNumber(value) {
    if (value === null || value === undefined) return "—";
    return new Intl.NumberFormat("en-US").format(value);
}

export function formatCurrency(value) {
    if (value === null || value === undefined) return "—";
    return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
}
