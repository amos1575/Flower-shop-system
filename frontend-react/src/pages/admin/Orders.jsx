import { useEffect, useState } from "react";
import { apiRequest, formatMoney, formatDate } from "../../api/client";
import Alert from "../../components/Alert";

const STATUSES = ["PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"];

export default function AdminOrders() {
    const [orders, setOrders] = useState(null);
    const [selected, setSelected] = useState({});
    const [error, setError] = useState("");

    async function loadOrders() {
        try {
            const page = await apiRequest("/orders?size=100&sort=id,desc");
            setOrders(page.content);
            setSelected((prev) => {
                const next = { ...prev };
                page.content.forEach((o) => {
                    if (!(o.id in next)) next[o.id] = o.status;
                });
                return next;
            });
        } catch (err) {
            setError(err.message);
        }
    }

    useEffect(() => {
        loadOrders();
    }, []);

    function handleStatusChange(orderId, value) {
        setSelected((prev) => ({ ...prev, [orderId]: value }));
    }

    async function handleUpdate(orderId) {
        setError("");
        try {
            await apiRequest(`/orders/${orderId}/status`, { method: "PATCH", body: { status: selected[orderId] } });
            loadOrders();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <>
            <h1>Orders</h1>
            <Alert message={error} />
            <div className="table-wrap">
                <table>
                    <thead>
                        <tr><th>ID</th><th>Customer</th><th>Placed</th><th>Total</th><th>Status</th><th>Update</th></tr>
                    </thead>
                    <tbody>
                        {orders === null ? (
                            <tr><td colSpan="6" className="muted">Loading…</td></tr>
                        ) : orders.length === 0 ? (
                            <tr><td colSpan="6" className="muted">No orders yet.</td></tr>
                        ) : (
                            orders.map((order) => {
                                const itemsTitle = order.items.map((i) => `${i.quantity}x ${i.flowerName}`).join(", ");
                                return (
                                    <tr key={order.id}>
                                        <td>#{order.id}</td>
                                        <td>{order.customerName}</td>
                                        <td>{formatDate(order.createdAt)}</td>
                                        <td title={itemsTitle}>{formatMoney(order.totalAmount)}</td>
                                        <td><span className={`badge badge-${order.status}`}>{order.status}</span></td>
                                        <td>
                                            <select
                                                value={selected[order.id] || order.status}
                                                onChange={(e) => handleStatusChange(order.id, e.target.value)}
                                            >
                                                {STATUSES.map((s) => (
                                                    <option key={s} value={s}>{s}</option>
                                                ))}
                                            </select>{" "}
                                            <button className="btn btn-sm btn-secondary" onClick={() => handleUpdate(order.id)}>Update</button>
                                        </td>
                                    </tr>
                                );
                            })
                        )}
                    </tbody>
                </table>
            </div>
        </>
    );
}
