import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { apiRequest, formatMoney, formatDate } from "../../api/client";
import Alert from "../../components/Alert";

async function loadDeliveryStatus(orderId) {
    try {
        const delivery = await apiRequest(`/deliveries/order/${orderId}`);
        return delivery.status;
    } catch {
        return null;
    }
}

export default function CustomerOrders() {
    const [searchParams] = useSearchParams();
    const placedId = searchParams.get("placed");

    const [orders, setOrders] = useState(null);
    const [deliveryStatuses, setDeliveryStatuses] = useState({});
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadOrders() {
            try {
                const page = await apiRequest("/orders/my?size=50&sort=id,desc");
                if (cancelled) return;
                setOrders(page.content);

                const entries = await Promise.all(
                    page.content.map(async (order) => [order.id, await loadDeliveryStatus(order.id)])
                );
                if (!cancelled) {
                    setDeliveryStatuses(Object.fromEntries(entries));
                }
            } catch (err) {
                if (!cancelled) setError(err.message);
            }
        }

        loadOrders();
        return () => { cancelled = true; };
    }, []);

    return (
        <>
            <h1>My Orders</h1>
            {placedId && <Alert message={`Order #${placedId} placed successfully!`} type="success" />}
            <Alert message={error} />

            {orders === null ? (
                <p className="muted">Loading…</p>
            ) : orders.length === 0 ? (
                <div className="empty-state">You haven't placed any orders yet. <Link to="/customer">Start shopping</Link>.</div>
            ) : (
                orders.map((order) => {
                    const deliveryStatus = deliveryStatuses[order.id];
                    return (
                        <div className="card order-card" key={order.id}>
                            <div className="flex-between">
                                <strong>Order #{order.id}</strong>
                                <span className={`badge badge-${order.status}`}>{order.status}</span>
                            </div>
                            <p className="muted">{formatDate(order.createdAt)} · {order.deliveryAddress}</p>
                            <ul style={{ margin: "0.5rem 0", paddingLeft: "1.2rem", color: "var(--color-text-muted)", fontSize: "0.9rem" }}>
                                {order.items.map((i) => (
                                    <li key={i.flowerId}>{i.quantity} × {i.flowerName} ({formatMoney(i.lineTotal)})</li>
                                ))}
                            </ul>
                            <p><strong>Total: {formatMoney(order.totalAmount)}</strong></p>
                            {deliveryStatus && (
                                <p className="muted">Delivery: <span className={`badge badge-${deliveryStatus}`}>{deliveryStatus}</span></p>
                            )}
                        </div>
                    );
                })
            )}
        </>
    );
}
