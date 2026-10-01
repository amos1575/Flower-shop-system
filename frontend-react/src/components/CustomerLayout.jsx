import { Outlet } from "react-router-dom";
import CustomerNav from "./CustomerNav";

export default function CustomerLayout() {
    return (
        <>
            <CustomerNav />
            <main className="container">
                <Outlet />
            </main>
        </>
    );
}
