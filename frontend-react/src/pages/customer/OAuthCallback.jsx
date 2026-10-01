import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import Alert from "../../components/Alert";

export default function CustomerOAuthCallback() {
    const { completeOAuthLogin } = useAuth();
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [error, setError] = useState("");
    const ran = useRef(false);

    useEffect(() => {
        if (ran.current) return;
        ran.current = true;

        const token = searchParams.get("token");
        if (!token) {
            setError("Missing login token.");
            return;
        }
        completeOAuthLogin(token)
            .then(() => navigate("/customer", { replace: true }))
            .catch((err) => setError(err.message));
    }, [searchParams, completeOAuthLogin, navigate]);

    return (
        <main className="container" style={{ paddingTop: "3rem" }}>
            {error ? (
                <>
                    <Alert message={error} />
                    <p className="muted"><Link to="/customer/login">Back to login</Link></p>
                </>
            ) : (
                <p className="muted">Signing you in…</p>
            )}
        </main>
    );
}
