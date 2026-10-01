import { Outlet } from "react-router-dom";
import DeliveryNav from "./DeliveryNav";

export default function DeliveryLayout() {
    return (
        <>
            <DeliveryNav />
            <main className="container">
                <Outlet />
            </main>
        </>
    );
}
