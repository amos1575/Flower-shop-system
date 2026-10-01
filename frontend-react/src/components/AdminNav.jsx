import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function AdminNav() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const linkClass = ({ isActive }) => (isActive ? "active" : "");

    function handleLogout(e) {
        e.preventDefault();
        logout();
        navigate("/admin/login");
    }

    return (
        <div className="navbar">
            <div className="container">
                <NavLink to="/admin" end className="navbar-brand">🌸 Flower Shop Admin</NavLink>
                <div className="navbar-links">
                    <NavLink to="/admin" end className={linkClass}>Dashboard</NavLink>
                    <NavLink to="/admin/categories" className={linkClass}>Categories</NavLink>
                    <NavLink to="/admin/flowers" className={linkClass}>Flowers</NavLink>
                    <NavLink to="/admin/orders" className={linkClass}>Orders</NavLink>
                    <NavLink to="/admin/deliveries" className={linkClass}>Deliveries</NavLink>
                    <NavLink to="/admin/staff" className={linkClass}>Staff</NavLink>
                    <NavLink to="/admin/reports" className={linkClass}>Reports</NavLink>
                    {user ? (
                        <>
                            <span className="navbar-user">{user.fullName}</span>
                            <a href="#" onClick={handleLogout}>Logout</a>
                        </>
                    ) : (
                        <NavLink to="/admin/login" className={linkClass}>Login</NavLink>
                    )}
                </div>
            </div>
        </div>
    );
}
