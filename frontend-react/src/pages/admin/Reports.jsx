import { useEffect, useState } from "react";
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

function fmt(d) {
    return d.toISOString().slice(0, 10);
}

export default function AdminReports() {
    const today = new Date();
    const yearAgo = new Date(today);
    yearAgo.setDate(yearAgo.getDate() - 365);

    const [from, setFrom] = useState(fmt(yearAgo));
    const [to, setTo] = useState(fmt(today));
    const [salesReport, setSalesReport] = useState(null);
    const [inventoryReport, setInventoryReport] = useState(null);
    const [error, setError] = useState("");

    async function loadSalesReport(fromValue, toValue) {
        try {
            const report = await apiRequest(`/reports/sales?from=${fromValue}&to=${toValue}`);
            setSalesReport(report);
        } catch (err) {
            setError(err.message);
        }
    }

    async function loadInventoryReport() {
        try {
            const report = await apiRequest("/reports/inventory");
            setInventoryReport(report);
        } catch (err) {
            setError(err.message);
        }
    }

    useEffect(() => {
        loadSalesReport(from, to);
        loadInventoryReport();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    function handleSalesSubmit(e) {
        e.preventDefault();
        loadSalesReport(from, to);
    }

    return (
        <>
            <h1>Reports</h1>
            <Alert message={error} />

            <h2>Sales report</h2>
            <form className="form form-inline mb-1" onSubmit={handleSalesSubmit}>
                <div className="form-group">
                    <label htmlFor="from">From</label>
                    <input type="date" id="from" value={from} onChange={(e) => setFrom(e.target.value)} required />
                </div>
                <div className="form-group">
                    <label htmlFor="to">To</label>
                    <input type="date" id="to" value={to} onChange={(e) => setTo(e.target.value)} required />
                </div>
                <button type="submit" className="btn btn-secondary">Run report</button>
            </form>

            {salesReport && (
                <>
                    <div className="stat-grid mb-1">
                        <StatCard value={salesReport.totalOrders} label="Total orders" />
                        <StatCard value={formatMoney(salesReport.totalRevenue)} label="Total revenue" />
                        <StatCard value={formatMoney(salesReport.averageOrderValue)} label="Average order value" />
                    </div>
                    <div>
                        {salesReport.topFlowers.length === 0 ? (
                            <p className="muted">No sales in this period.</p>
                        ) : (
                            <div className="table-wrap">
                                <table>
                                    <thead>
                                        <tr><th>Flower</th><th>Units sold</th><th>Revenue</th></tr>
                                    </thead>
                                    <tbody>
                                        {salesReport.topFlowers.map((f) => (
                                            <tr key={f.flowerName}>
                                                <td>{f.flowerName}</td>
                                                <td>{f.quantitySold}</td>
                                                <td>{formatMoney(f.revenue)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </>
            )}

            <h2 className="mt-2">Inventory report</h2>
            {inventoryReport && (
                <>
                    <div className="stat-grid mb-1">
                        <StatCard value={inventoryReport.totalActiveFlowers} label="Active flowers" />
                        <StatCard value={inventoryReport.totalStockUnits} label="Units in stock" />
                        <StatCard value={formatMoney(inventoryReport.totalStockValue)} label="Stock value" />
                    </div>
                    <div>
                        {inventoryReport.lowStockFlowers.length === 0 ? (
                            <p className="muted">No flowers below the {inventoryReport.lowStockThreshold}-unit threshold.</p>
                        ) : (
                            <>
                                <h2>Low stock (below {inventoryReport.lowStockThreshold} units)</h2>
                                <div className="table-wrap">
                                    <table>
                                        <thead>
                                            <tr><th>Flower</th><th>Stock</th></tr>
                                        </thead>
                                        <tbody>
                                            {inventoryReport.lowStockFlowers.map((f) => (
                                                <tr key={f.name}>
                                                    <td>{f.name}</td>
                                                    <td>{f.stockQuantity}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </>
                        )}
                    </div>
                </>
            )}
        </>
    );
}
