import { useEffect, useState } from "react";
import { apiRequest, formatDate } from "../../api/client";
import Alert from "../../components/Alert";

const STATUSES = ["ASSIGNED", "OUT_FOR_DELIVERY", "DELIVERED", "FAILED"];

export default function DeliveryDeliveries() {
    const [deliveries, setDeliveries] = useState(null);
    const [error, setError] = useState("");
    const [drafts, setDrafts] = useState({});

    async function loadDeliveries() {
        try {
            const page = await apiRequest("/deliveries/my?size=50&sort=id,desc");
            setDeliveries(page.content);
            const nextDrafts = {};
            page.content.forEach((d) => {
                nextDrafts[d.id] = { status: d.status, notes: "" };
            });
            setDrafts(nextDrafts);
        } catch (err) {
            setError(err.message);
        }
    }

    useEffect(() => {
        loadDeliveries();
    }, []);

    function handleStatusChange(id, status) {
        setDrafts((current) => ({ ...current, [id]: { ...current[id], status } }));
    }

    function handleNotesChange(id, notes) {
        setDrafts((current) => ({ ...current, [id]: { ...current[id], notes } }));
    }

    async function handleUpdate(id) {
        setError("");
        const draft = drafts[id];
        try {
            await apiRequest(`/deliveries/${id}/status`, {
                method: "PATCH",
                body: { status: draft.status, notes: draft.notes.trim() || null },
            });
            loadDeliveries();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <>
            <h1>My Deliveries</h1>
            <Alert message={error} />

            {deliveries === null ? (
                <p className="muted">Loading…</p>
            ) : deliveries.length === 0 ? (
                <div className="empty-state">No deliveries assigned to you yet.</div>
            ) : (
                deliveries.map((d) => (
                    <div className="card delivery-card" key={d.id}>
                        <div className="flex-between">
                            <strong>Delivery #{d.id} — Order #{d.orderId}</strong>
                            <span className={`badge badge-${d.status}`}>{d.status}</span>
                        </div>
                        <p className="muted">Assigned: {formatDate(d.assignedAt)}</p>
                        {d.notes ? <p>Notes: {d.notes}</p> : null}
                        <div className="form form-inline mt-1">
                            <div className="form-group">
                                <label>Update status</label>
                                <select
                                    value={drafts[d.id]?.status ?? d.status}
                                    onChange={(e) => handleStatusChange(d.id, e.target.value)}
                                >
                                    {STATUSES.map((s) => (
                                        <option key={s} value={s}>{s}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="form-group" style={{ flex: 2 }}>
                                <label>Notes (optional)</label>
                                <input
                                    type="text"
                                    placeholder="e.g. Left at front door"
                                    value={drafts[d.id]?.notes ?? ""}
                                    onChange={(e) => handleNotesChange(d.id, e.target.value)}
                                />
                            </div>
                            <button className="btn btn-secondary" onClick={() => handleUpdate(d.id)}>Update</button>
                        </div>
                    </div>
                ))
            )}
        </>
    );
}
