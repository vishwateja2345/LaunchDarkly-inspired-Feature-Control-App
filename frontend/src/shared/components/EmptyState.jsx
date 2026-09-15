import { MaterialIcon } from "./MaterialIcon.jsx";

export function EmptyState({ action, description, icon = "folder", title }) {
    return (
        <div className="empty-state">
            <MaterialIcon className="empty-state-icon" size={40}>
                {icon}
            </MaterialIcon>
            <h3>{title}</h3>
            {description && <p>{description}</p>}
            {action && <div className="empty-state-action">{action}</div>}
        </div>
    );
}
