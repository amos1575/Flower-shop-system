import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ role, loginPath }) {
    const { user } = useAuth();

    if (!user || user.role !== role) {
        return <Navigate to={loginPath} replace />;
    }

    return <Outlet />;
}
