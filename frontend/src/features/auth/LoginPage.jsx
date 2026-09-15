import { useState } from "react";
import { useAuth } from "../../shared/AuthContext.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { identityColor, identityInitials } from "../../shared/utils/identity.js";

const DEMO_PASSWORD = "password123";

const DEMO_ACCOUNTS = [
    { name: "Alex Morgan", email: "alex.morgan@flagdeck.com", role: "Admin" },
    { name: "Jordan Lee", email: "jordan.lee@flagdeck.com", role: "Member" },
    { name: "Sam Rivera", email: "sam.rivera@flagdeck.com", role: "Member" },
    { name: "Taylor Chen", email: "taylor.chen@flagdeck.com", role: "Member" },
    { name: "Priya Patel", email: "priya.patel@flagdeck.com", role: "Member" },
];

export function LoginPage() {
    const { login } = useAuth();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [quickLoadingEmail, setQuickLoadingEmail] = useState("");

    const busy = loading || Boolean(quickLoadingEmail);

    const submit = async (event) => {
        event.preventDefault();
        setError("");
        setLoading(true);
        try {
            await login(email.trim(), password);
        } catch (caught) {
            setError(caught.message || "Unable to sign in.");
        } finally {
            setLoading(false);
        }
    };

    const quickLogin = async (demoEmail) => {
        setError("");
        setEmail(demoEmail);
        setPassword(DEMO_PASSWORD);
        setQuickLoadingEmail(demoEmail);
        try {
            await login(demoEmail, DEMO_PASSWORD);
        } catch (caught) {
            setError(caught.message || "Unable to sign in.");
        } finally {
            setQuickLoadingEmail("");
        }
    };

    return (
        <main className="login-page">
            <section className="login-card" aria-labelledby="login-title">
                <div className="login-mark">
                    <MaterialIcon size={26}>flag</MaterialIcon>
                </div>
                <h1 id="login-title">Sign in to FlagDeck</h1>
                <p className="login-subtitle">Feature flags, targeting, and rollouts for your team</p>
                <form onSubmit={submit}>
                    <div className="outlined-input">
                        <input
                            autoComplete="username"
                            data-testid="email-input"
                            disabled={busy}
                            id="login-email"
                            inputMode="email"
                            placeholder=" "
                            required
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                        />
                        <label htmlFor="login-email">Email</label>
                    </div>
                    <div className="outlined-input">
                        <input
                            autoComplete="current-password"
                            data-testid="password-input"
                            disabled={busy}
                            id="login-password"
                            placeholder=" "
                            required
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                        />
                        <label htmlFor="login-password">Password</label>
                    </div>
                    {error && (
                        <p className="login-error" role="alert">
                            {error}
                        </p>
                    )}
                    <div className="login-actions">
                        <button className="primary-button" disabled={busy} type="submit">
                            {loading ? "Signing in…" : "Sign in"}
                        </button>
                    </div>
                </form>

                <div className="login-demo-section">
                    <span className="login-demo-divider">or sign in as a demo teammate</span>
                    <div className="login-demo-list">
                        {DEMO_ACCOUNTS.map((account) => (
                            <button
                                className="login-demo-account"
                                disabled={busy}
                                key={account.email}
                                type="button"
                                onClick={() => quickLogin(account.email)}
                            >
                                <span className="avatar" style={{ background: identityColor(account.name) }}>
                                    {identityInitials(account.name)}
                                </span>
                                <span className="login-demo-account-info">
                                    <strong>{account.name}</strong>
                                    <span>{account.email}</span>
                                </span>
                                <span className="tag-chip">{account.role}</span>
                                {quickLoadingEmail === account.email ? (
                                    <span className="login-demo-spinner" aria-hidden="true" />
                                ) : (
                                    <MaterialIcon size={18}>chevron_right</MaterialIcon>
                                )}
                            </button>
                        ))}
                    </div>
                    <p className="login-demo-note">
                        Every seeded account shares the password <code>{DEMO_PASSWORD}</code> - pick two different
                        teammates in separate browser sessions to try the approval workflow.
                    </p>
                </div>
            </section>
        </main>
    );
}
