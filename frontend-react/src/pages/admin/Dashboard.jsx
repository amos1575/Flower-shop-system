import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest, formatMoney } from "../../api/client";
import Alert from "../../components/Alert";

function StatCard({ value, label }) {
    return (
        <div className="card stat-card">
            <div className="value">{value}</div>
            <div className="label">{label}</div>
        </div>
    );
}

export default function AdminDashboard() {
    const [stats, setStats] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        async function loadStats() {
            try {
                const today = new Date();
                const from = new Date(today);
                from.setDate(from.getDate() - 365);
                const fmt = (d) => d.toISOString().slice(0, 10);

                const [sales, inventory] = await Promise.all([
                    apiRequest(`/reports/sales?from=${fmt(from)}&to=${fmt(today)}`),
                    apiRequest("/reports/inventory"),
                ]);

                setStats({ sales, inventory });
            } catch (err) {
                setError(err.message);
            }
        }
        loadStats();
    }, []);

    return (
        <>
            <h1>Dashboard</h1>
            <Alert message={error} />
            <div className="stat-grid">
                {stats && (
                    <>
                        <StatCard value={stats.sales.totalOrders} label="Orders (last 365 days)" />
                        <StatCard value={formatMoney(stats.sales.totalRevenue)} label="Revenue (last 365 days)" />
                        <StatCard value={stats.inventory.totalActiveFlowers} label="Active flowers" />
                        <StatCard value={stats.inventory.totalStockUnits} label="Units in stock" />
                        <StatCard value={formatMoney(stats.inventory.totalStockValue)} label="Stock value" />
                        <StatCard value={stats.inventory.lowStockFlowers.length} label="Low-stock flowers" />
                    </>
                )}
            </div>

            <h2 className="mt-2">Quick links</h2>
            <div className="flex">
                <Link className="btn btn-secondary" to="/admin/categories">Manage Categories</Link>
                <Link className="btn btn-secondary" to="/admin/flowers">Manage Flowers</Link>
                <Link className="btn btn-secondary" to="/admin/orders">View Orders</Link>
                <Link className="btn btn-secondary" to="/admin/deliveries">Assign Deliveries</Link>
                <Link className="btn btn-secondary" to="/admin/staff">Manage Staff</Link>
            </div>
        </>
    );
}
