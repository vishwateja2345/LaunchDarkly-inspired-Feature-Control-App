export function identityInitials(name = "") {
    const parts = String(name).trim().split(/\s+/).filter(Boolean);
    return parts
        .slice(0, 2)
        .map((part) => part[0])
        .join("")
        .toUpperCase();
}

const AVATAR_PALETTE = ["#6366f1", "#0ea5e9", "#f59e0b", "#10b981", "#ec4899", "#8b5cf6", "#14b8a6"];

export function identityColor(seed = "") {
    let hash = 0;
    for (let index = 0; index < seed.length; index += 1) {
        hash = (hash * 31 + seed.charCodeAt(index)) >>> 0;
    }
    return AVATAR_PALETTE[hash % AVATAR_PALETTE.length];
}
