import { useEffect, useState } from "react";
import { apiRequest } from "../../api/client";
import Alert from "../../components/Alert";

export default function AdminDeliveries() {
    const [deliveryPersons, setDeliveryPersons] = useState([]);
    const [deliveries, setDeliveries] = useState(null);
    const [selected, setSelected] = useState({});
    const [error, setError] = useState("");

    async function loadDeliveryPersons() {
        const page = await apiRequest("/users?size=200");
        setDeliveryPersons(page.content.filter((u) => u.role === "DELIVERY" && u.active));
    }

    async function loadDeliveries() {
        try {
            const page = await apiRequest("/deliveries?size=100&sort=id,desc");
            setDeliveries(page.content);
        } catch (err) {
            setError(err.message);
        }
    }

    useEffect(() => {
        (async () => {
            try {
                await loadDeliveryPersons();
            } catch (err) {
                setError(err.message);
            }
            await loadDeliveries();
        })();
    }, []);

    function handleSelectChange(deliveryId, value) {
        setSelected((prev) => ({ ...prev, [deliveryId]: value }));
    }

    async function handleAssign(deliveryId) {
        setError("");
        const personId = selected[deliveryId];
        if (!personId) {
            setError("Choose a delivery person first.");
            return;
        }
        try {
            await apiRequest(`/deliveries/${deliveryId}/assign`, {
                method: "POST",
                body: { deliveryPersonId: Number(personId) },
            });
            loadDeliveries();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <>
            <h1>Deliveries</h1>
            <Alert message={error} />
            <div className="table-wrap">
                <table>
                    <thead>
                        <tr><th>Delivery</th><th>Order</th><th>Status</th><th>Assigned to</th><th>Assign</th></tr>
                    </thead>
                    <tbody>
                        {deliveries === null ? (
                            <tr><td colSpan="5" className="muted">Loading…</td></tr>
                        ) : deliveries.length === 0 ? (
                            <tr><td colSpan="5" className="muted">No deliveries yet.</td></tr>
                        ) : (
                            deliveries.map((d) => (
                                <tr key={d.id}>
                                    <td>#{d.id}</td>
                                    <td>Order #{d.orderId}</td>
                                    <td><span className={`badge badge-${d.status}`}>{d.status}</span></td>
                                    <td>{d.deliveryPersonName || "—"}</td>
                                    <td>
                                        <select
                                            value={selected[d.id] || ""}
                                            onChange={(e) => handleSelectChange(d.id, e.target.value)}
                                        >
                                            <option value="">Choose delivery person…</option>
                                            {deliveryPersons.map((u) => (
                                                <option key={u.id} value={u.id}>{u.fullName}</option>
                                            ))}
                                        </select>{" "}
                                        <button className="btn btn-sm btn-secondary" onClick={() => handleAssign(d.id)}>Assign</button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </>
    );
}
