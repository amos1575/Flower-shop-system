import { useEffect, useState } from "react";
import { apiRequest } from "../../api/client";
import Alert from "../../components/Alert";

const EMPTY_FORM = { fullName: "", email: "", password: "", role: "DELIVERY" };

export default function AdminStaff() {
    const [users, setUsers] = useState(null);
    const [error, setError] = useState("");
    const [form, setForm] = useState(EMPTY_FORM);

    async function loadUsers() {
        try {
            const page = await apiRequest("/users?size=200");
            setUsers(page.content);
        } catch (err) {
            setError(err.message);
        }
    }

    useEffect(() => {
        loadUsers();
    }, []);

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");

        const body = {
            fullName: form.fullName.trim(),
            email: form.email.trim(),
            password: form.password,
            role: form.role,
        };

        try {
            await apiRequest("/users/staff", { method: "POST", body });
            setForm(EMPTY_FORM);
            loadUsers();
        } catch (err) {
            setError(err.message);
        }
    }

    async function handleToggle(user) {
        setError("");
        try {
            await apiRequest(`/users/${user.id}/status`, {
                method: "PATCH",
                body: { active: !user.active },
            });
            loadUsers();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <>
            <h1>Staff &amp; Users</h1>
            <Alert message={error} />

            <h2>Create staff account</h2>
            <form className="form form-inline mb-1" onSubmit={handleSubmit}>
                <div className="form-group">
                    <label htmlFor="fullName">Full name</label>
                    <input
                        type="text"
                        id="fullName"
                        value={form.fullName}
                        onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="email">Email</label>
                    <input
                        type="email"
                        id="email"
                        value={form.email}
                        onChange={(e) => setForm({ ...form, email: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="password">Password</label>
                    <input
                        type="password"
                        id="password"
                        minLength="8"
                        value={form.password}
                        onChange={(e) => setForm({ ...form, password: e.target.value })}
                        required
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="role">Role</label>
                    <select id="role" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
                        <option value="DELIVERY">Delivery</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                </div>
                <button type="submit" className="btn btn-primary">Create</button>
            </form>

            <h2 className="mt-2">All users</h2>
            <div className="table-wrap">
                <table>
                    <thead>
                        <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr>
                    </thead>
                    <tbody>
                        {users === null ? (
                            <tr><td colSpan="5" className="muted">Loading…</td></tr>
                        ) : (
                            users.map((u) => (
                                <tr key={u.id}>
                                    <td>{u.fullName}</td>
                                    <td>{u.email}</td>
                                    <td>{u.role}</td>
                                    <td><span className={`badge ${u.active ? "badge-CONFIRMED" : "badge-CANCELLED"}`}>{u.active ? "Active" : "Inactive"}</span></td>
                                    <td>
                                        <button
                                            className={`btn btn-sm ${u.active ? "btn-danger" : "btn-secondary"}`}
                                            onClick={() => handleToggle(u)}
                                        >
                                            {u.active ? "Deactivate" : "Activate"}
                                        </button>
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
