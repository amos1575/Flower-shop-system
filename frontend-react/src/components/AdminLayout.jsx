import { Outlet } from "react-router-dom";
import AdminNav from "./AdminNav";

export default function AdminLayout() {
    return (
        <>
            <AdminNav />
            <main className="container">
                <Outlet />
            </main>
        </>
    );
}
