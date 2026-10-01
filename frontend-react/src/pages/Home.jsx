import { Link } from "react-router-dom";

export default function Home() {
    return (
        <main className="container" style={{ paddingTop: "3rem" }}>
            <h1>Flower Shop</h1>
            <p className="muted mb-1">Choose where you'd like to go.</p>

            <div className="grid">
                <Link className="card" to="/customer">
                    <h2>Customer</h2>
                    <p className="muted">Browse flowers and place orders.</p>
                </Link>
                <Link className="card" to="/admin">
                    <h2>Admin</h2>
                    <p className="muted">Manage inventory, orders and users.</p>
                </Link>
                <Link className="card" to="/delivery">
                    <h2>Delivery</h2>
                    <p className="muted">Track and update deliveries.</p>
                </Link>
            </div>
        </main>
    );
}
