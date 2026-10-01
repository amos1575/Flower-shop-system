import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";

export default function CustomerNav() {
    const { user, logout } = useAuth();
    const { count } = useCart();
    const navigate = useNavigate();

    const linkClass = ({ isActive }) => (isActive ? "active" : "");

    function handleLogout(e) {
        e.preventDefault();
        logout();
        navigate("/customer/login");
    }

    return (
        <div className="navbar">
            <div className="container">
                <NavLink to="/customer" end className="navbar-brand">🌸 Flower Shop</NavLink>
                <div className="navbar-links">
                    <NavLink to="/customer" end className={linkClass}>Shop</NavLink>
                    {user && <NavLink to="/customer/orders" className={linkClass}>My Orders</NavLink>}
                    <NavLink to="/customer/cart" className={linkClass}>Cart ({count})</NavLink>
                    {user ? (
                        <>
                            <span className="navbar-user">Hi, {user.fullName}</span>
                            <a href="#" onClick={handleLogout}>Logout</a>
                        </>
                    ) : (
                        <>
                            <NavLink to="/customer/login" className={linkClass}>Login</NavLink>
                            <NavLink to="/customer/register" className={linkClass}>Register</NavLink>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
