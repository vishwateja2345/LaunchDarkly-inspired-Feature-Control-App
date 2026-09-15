import { Component } from "react";
import { MaterialIcon } from "./MaterialIcon.jsx";

/**
 * Last-resort safety net. React unmounts a whole tree on an uncaught render error, which
 * without this would leave a blank page - this catches that and offers a way back instead.
 */
export class ErrorBoundary extends Component {
    constructor(props) {
        super(props);
        this.state = { error: null };
    }

    static getDerivedStateFromError(error) {
        return { error };
    }

    componentDidCatch(error, info) {
        // Keep this as a real console.error (not silenced) so it still surfaces in dev tools.
        console.error("FlagDeck crashed:", error, info.componentStack);
    }

    render() {
        if (!this.state.error) {
            return this.props.children;
        }

        return (
            <div className="app-boot">
                <div className="error-boundary-card">
                    <MaterialIcon size={32}>warning_amber</MaterialIcon>
                    <h2>Something went wrong</h2>
                    <p>
                        FlagDeck hit an unexpected error. Your data is safe - reloading usually
                        recovers the page.
                    </p>
                    <button className="primary-button" type="button" onClick={() => window.location.reload()}>
                        Reload FlagDeck
                    </button>
                </div>
            </div>
        );
    }
}
