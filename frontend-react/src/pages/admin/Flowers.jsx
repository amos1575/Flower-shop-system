import { useEffect, useState } from "react";
import { apiRequest, formatMoney } from "../../api/client";
import Alert from "../../components/Alert";

const EMPTY_FORM = { id: "", name: "", categoryId: "", description: "", price: "", stockQuantity: "", imageUrl: "" };

export default function AdminFlowers() {
    const [categories, setCategories] = useState([]);
    const [flowers, setFlowers] = useState(null);
    const [error, setError] = useState("");
    const [form, setForm] = useState(EMPTY_FORM);

    const isEditing = Boolean(form.id);

    async function loadCategories() {
        const data = await apiRequest("/categories");
        setCategories(data);
        return data;
    }

    async function loadFlowers() {
        try {
            const page = await apiRequest("/flowers?includeInactive=true&size=100");
            setFlowers(page.content);
        } catch (err) {
            setError(err.message);
        }
    }

    useEffect(() => {
        (async () => {
            try {
                const cats = await loadCategories();
                if (cats.length > 0) {
                    setForm((f) => ({ ...f, categoryId: f.categoryId || cats[0].id }));
                }
            } catch (err) {
                setError(err.message);
            }
            await loadFlowers();
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    function startEdit(f) {
        setForm({
            id: f.id,
            name: f.name,
            categoryId: f.categoryId,
            description: f.description || "",
            price: f.price,
            stockQuantity: f.stockQuantity,
            imageUrl: f.imageUrl || "",
        });
        window.scrollTo({ top: 0, behavior: "smooth" });
    }

    function resetForm() {
        setForm({ ...EMPTY_FORM, categoryId: categories[0]?.id || "" });
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");

        const body = {
            categoryId: Number(form.categoryId),
            name: form.name.trim(),
            description: form.description.trim() || null,
            price: Number(form.price),
            stockQuantity: Number(form.stockQuantity),
            imageUrl: form.imageUrl.trim() || null,
        };

        try {
            if (isEditing) {
                await apiRequest(`/flowers/${form.id}`, { method: "PUT", body });
            } else {
                await apiRequest("/flowers", { method: "POST", body });
            }
            resetForm();
            loadFlowers();
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleDeactivate(id) {
        if (!confirm("Deactivate this flower? It will be hidden from the storefront.")) return;
        setError("");
        try {
            await apiRequest(`/flowers/${id}`, { method: "DELETE" });
            loadFlowers();
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleReactivate(id) {
        setError("");
        try {
            await apiRequest(`/flowers/${id}/reactivate`, { method: "PATCH" });
            loadFlowers();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <>
            <h1>Flowers</h1>
            <Alert message={error} />

            <form className="form mb-1" onSubmit={handleSubmit}>
                <div className="form-row">
                    <div className="form-group">
                        <label htmlFor="name">Name</label>
                        <input
                            type="text"
                            id="name"
                            value={form.name}
                            onChange={(e) => setForm({ ...form, name: e.target.value })}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="category">Category</label>
                        <select
                            id="category"
                            value={form.categoryId}
                            onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
                            required
                        >
                            {categories.map((c) => (
                                <option key={c.id} value={c.id}>{c.name}</option>
                            ))}
                        </select>
                    </div>
                </div>
                <div className="form-group">
                    <label htmlFor="description">Description</label>
                    <input
                        type="text"
                        id="description"
                        value={form.description}
                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                    />
                </div>
                <div className="form-row">
                    <div className="form-group">
                        <label htmlFor="price">Price</label>
                        <input
                            type="number"
                            id="price"
                            min="0"
                            step="0.01"
                            value={form.price}
                            onChange={(e) => setForm({ ...form, price: e.target.value })}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="stockQuantity">Stock quantity</label>
                        <input
                            type="number"
                            id="stockQuantity"
                            min="0"
                            value={form.stockQuantity}
                            onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })}
                            required
                        />
                    </div>
                </div>
                <div className="form-group">
                    <label htmlFor="imageUrl">Image URL</label>
                    <input
                        type="text"
                        id="imageUrl"
                        value={form.imageUrl}
                        onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                    />
                </div>
                <div className="flex">
                    <button type="submit" className="btn btn-primary">{isEditing ? "Save changes" : "Add flower"}</button>
                    {isEditing && (
                        <button type="button" className="btn btn-secondary" onClick={resetForm}>Cancel</button>
                    )}
                </div>
            </form>

            <div className="table-wrap">
                <table>
                    <thead>
                        <tr><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th>Status</th><th></th></tr>
                    </thead>
                    <tbody>
                        {flowers === null ? (
                            <tr><td colSpan="6" className="muted">Loading…</td></tr>
                        ) : flowers.length === 0 ? (
                            <tr><td colSpan="6" className="muted">No flowers yet.</td></tr>
                        ) : (
                            flowers.map((f) => (
                                <tr key={f.id}>
                                    <td>{f.name}</td>
                                    <td>{f.categoryName}</td>
                                    <td>{formatMoney(f.price)}</td>
                                    <td>{f.stockQuantity}</td>
                                    <td><span className={`badge ${f.active ? "badge-CONFIRMED" : "badge-CANCELLED"}`}>{f.active ? "Active" : "Inactive"}</span></td>
                                    <td>
                                        <button className="btn btn-sm btn-secondary" onClick={() => startEdit(f)}>Edit</button>{" "}
                                        {f.active ? (
                                            <button className="btn btn-sm btn-danger" onClick={() => handleDeactivate(f.id)}>Deactivate</button>
                                        ) : (
                                            <button className="btn btn-sm btn-secondary" onClick={() => handleReactivate(f.id)}>Reactivate</button>
                                        )}
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
