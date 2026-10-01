import { useEffect, useState } from "react";
import { apiRequest } from "../../api/client";
import Alert from "../../components/Alert";

const EMPTY_FORM = { id: "", name: "", description: "" };

export default function AdminCategories() {
    const [categories, setCategories] = useState(null);
    const [error, setError] = useState("");
    const [form, setForm] = useState(EMPTY_FORM);

    const isEditing = Boolean(form.id);

    async function loadCategories() {
        try {
            const data = await apiRequest("/categories");
            setCategories(data);
        } catch (err) {
            setError(err.message);
        }
    }

    useEffect(() => {
        loadCategories();
    }, []);

    function startEdit(c) {
        setForm({ id: c.id, name: c.name, description: c.description || "" });
    }

    function resetForm() {
        setForm(EMPTY_FORM);
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");

        const body = { name: form.name.trim(), description: form.description.trim() || null };

        try {
            if (isEditing) {
                await apiRequest(`/categories/${form.id}`, { method: "PUT", body });
            } else {
                await apiRequest("/categories", { method: "POST", body });
            }
            resetForm();
            loadCategories();
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleDelete(id) {
        setError("");
        if (!confirm("Delete this category?")) return;
        try {
            await apiRequest(`/categories/${id}`, { method: "DELETE" });
            loadCategories();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <>
            <h1>Categories</h1>
            <Alert message={error} />

            <form className="form form-inline mb-1" onSubmit={handleSubmit}>
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
                <div className="form-group" style={{ flex: 2 }}>
                    <label htmlFor="description">Description</label>
                    <input
                        type="text"
                        id="description"
                        value={form.description}
                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                    />
                </div>
                <button type="submit" className="btn btn-primary">{isEditing ? "Save changes" : "Add category"}</button>
                {isEditing && (
                    <button type="button" className="btn btn-secondary" onClick={resetForm}>Cancel</button>
                )}
            </form>

            <div className="table-wrap">
                <table>
                    <thead>
                        <tr><th>Name</th><th>Description</th><th>Flowers</th><th></th></tr>
                    </thead>
                    <tbody>
                        {categories === null ? (
                            <tr><td colSpan="4" className="muted">Loading…</td></tr>
                        ) : categories.length === 0 ? (
                            <tr><td colSpan="4" className="muted">No categories yet.</td></tr>
                        ) : (
                            categories.map((c) => (
                                <tr key={c.id}>
                                    <td>{c.name}</td>
                                    <td>{c.description || ""}</td>
                                    <td>{c.flowerCount}</td>
                                    <td>
                                        <button className="btn btn-sm btn-secondary" onClick={() => startEdit(c)}>Edit</button>{" "}
                                        <button className="btn btn-sm btn-danger" onClick={() => handleDelete(c.id)}>Delete</button>
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
