import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function DeliveryNav() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const linkClass = ({ isActive }) => (isActive ? "active" : "");

    function handleLogout(e) {
        e.preventDefault();
        logout();
        navigate("/delivery/login");
    }

    return (
        <div className="navbar">
            <div className="container">
                <NavLink to="/delivery" end className="navbar-brand">🌸 Flower Shop Delivery</NavLink>
                <div className="navbar-links">
                    <NavLink to="/delivery" end className={linkClass}>My Deliveries</NavLink>
                    {user ? (
                        <>
                            <span className="navbar-user">{user.fullName}</span>
                            <a href="#" onClick={handleLogout}>Logout</a>
                        </>
                    ) : (
                        <NavLink to="/delivery/login" className={linkClass}>Login</NavLink>
                    )}
                </div>
            </div>
        </div>
    );
}
