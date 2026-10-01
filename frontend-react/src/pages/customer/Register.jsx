import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import Alert from "../../components/Alert";
import AuthSplit from "../../components/AuthSplit";

export default function CustomerRegister() {
    const { register } = useAuth();
    const navigate = useNavigate();
    const [fullName, setFullName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [phone, setPhone] = useState("");
    const [address, setAddress] = useState("");
    const [error, setError] = useState("");

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");

        const body = {
            fullName: fullName.trim(),
            email: email.trim(),
            password,
            phone: phone.trim() || null,
            address: address.trim() || null,
        };

        try {
            await register(body);
            navigate("/customer");
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <AuthSplit backTo="/customer" tagline="Every bouquet starts with hello.">
            <div className="auth-logo" aria-hidden="true">🌸</div>
            <h1>Create an account</h1>
            <Alert message={error} />
            <form className="form" onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="fullName">Full name</label>
                    <div className="field-icon">
                        <input id="fullName" type="text" placeholder="Your name" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
                        <span className="field-icon-static" aria-hidden="true">🧑</span>
                    </div>
                </div>
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
                        <input id="password" type={showPassword ? "text" : "password"} placeholder="At least 8 characters" minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} required />
                        <button type="button" className="field-icon-btn" onClick={() => setShowPassword((v) => !v)} aria-label={showPassword ? "Hide password" : "Show password"}>
                            {showPassword ? "🙈" : "👁"}
                        </button>
                    </div>
                </div>
                <div className="form-group">
                    <label htmlFor="phone">Phone (optional)</label>
                    <div className="field-icon">
                        <input id="phone" type="text" placeholder="Insert phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
                        <span className="field-icon-static" aria-hidden="true">📞</span>
                    </div>
                </div>
                <div className="form-group">
                    <label htmlFor="address">Address (optional)</label>
                    <div className="field-icon">
                        <input id="address" type="text" placeholder="Delivery address" value={address} onChange={(e) => setAddress(e.target.value)} />
                        <span className="field-icon-static" aria-hidden="true">📍</span>
                    </div>
                </div>
                <button type="submit" className="btn btn-primary">Register</button>
            </form>
            <p className="auth-footer-link">Already have an account? <Link to="/customer/login">Log in</Link>.</p>
        </AuthSplit>
    );
}
