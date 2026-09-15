import { useState } from "react";
import { useAuth } from "../../shared/AuthContext.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";

const DEMO_ACCOUNTS = [
    "alex.morgan@flagdeck.com",
    "jordan.lee@flagdeck.com",
    "sam.rivera@flagdeck.com",
    "taylor.chen@flagdeck.com",
    "priya.patel@flagdeck.com",
];

export function LoginPage() {
    const { login } = useAuth();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

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
                            disabled={loading}
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
                            disabled={loading}
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
                        <button className="primary-button" disabled={loading} type="submit">
                            {loading ? "Signing in…" : "Sign in"}
                        </button>
                    </div>
                </form>
                <details className="login-demo-hint">
                    <summary>Demo accounts</summary>
                    <p>Every seeded account uses the password: <code>password123</code></p>
                    <ul>
                        {DEMO_ACCOUNTS.map((demoEmail) => (
                            <li key={demoEmail}>
                                <button type="button" onClick={() => setEmail(demoEmail)}>
                                    {demoEmail}
                                </button>
                            </li>
                        ))}
                    </ul>
                </details>
            </section>
        </main>
    );
}
