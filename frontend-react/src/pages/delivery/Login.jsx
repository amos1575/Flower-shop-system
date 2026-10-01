import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import Alert from "../../components/Alert";
import AuthSplit from "../../components/AuthSplit";

export default function DeliveryLogin() {
    const { login } = useAuth();
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        try {
            await login(email, password, "DELIVERY");
            navigate("/delivery");
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <AuthSplit backTo="/" tagline="Every delivery, a little joy on someone's doorstep.">
            <div className="auth-logo" aria-hidden="true">🌸</div>
            <h1>Flower Shop Delivery</h1>
            <Alert message={error} />
            <form className="form" onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="email">Email</label>
                    <div className="field-icon">
                        <input id="email" type="email" placeholder="Insert email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                        <span className="field-icon-static" aria-hidden="true">✉</span>
                    </div>
                </div>
                <div className="form-group">
                    <label htmlFor="password">Password</label>
                    <div className="field-icon">
                        <input id="password" type={showPassword ? "text" : "password"} placeholder="Enter password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                        <button type="button" className="field-icon-btn" onClick={() => setShowPassword((v) => !v)} aria-label={showPassword ? "Hide password" : "Show password"}>
                            {showPassword ? "🙈" : "👁"}
                        </button>
                    </div>
                </div>
                <button type="submit" className="btn btn-primary">Login</button>
            </form>
        </AuthSplit>
    );
}
