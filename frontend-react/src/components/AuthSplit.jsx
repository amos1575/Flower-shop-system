import { Link } from "react-router-dom";

export default function AuthSplit({ tagline, backTo = "/", children }) {
    return (
        <div className="auth-split">
            <div className="auth-visual">
                <div className="auth-visual-glow" aria-hidden="true" />
                <div className="auth-visual-bloom" aria-hidden="true">🌸</div>
                {tagline && <p className="auth-visual-tagline">{tagline}</p>}
            </div>
            <div className="auth-panel">
                <Link to={backTo} className="auth-back">← Back to site</Link>
                <div className="auth-panel-inner">{children}</div>
            </div>
        </div>
    );
}
