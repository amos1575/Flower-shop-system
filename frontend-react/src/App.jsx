import { Routes, Route } from "react-router-dom";

import Home from "./pages/Home";

import CustomerLayout from "./components/CustomerLayout";
import AdminLayout from "./components/AdminLayout";
import DeliveryLayout from "./components/DeliveryLayout";
import ProtectedRoute from "./components/ProtectedRoute";

import CustomerShop from "./pages/customer/Shop";
import CustomerLogin from "./pages/customer/Login";
import CustomerRegister from "./pages/customer/Register";
import CustomerProduct from "./pages/customer/Product";
import CustomerCart from "./pages/customer/Cart";
import CustomerOrders from "./pages/customer/Orders";
import CustomerOAuthCallback from "./pages/customer/OAuthCallback";

import AdminLogin from "./pages/admin/Login";
import AdminDashboard from "./pages/admin/Dashboard";
import AdminCategories from "./pages/admin/Categories";
import AdminFlowers from "./pages/admin/Flowers";
import AdminOrders from "./pages/admin/Orders";
import AdminDeliveries from "./pages/admin/Deliveries";
import AdminStaff from "./pages/admin/Staff";
import AdminReports from "./pages/admin/Reports";

import DeliveryLogin from "./pages/delivery/Login";
import DeliveryDeliveries from "./pages/delivery/Deliveries";

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<Home />} />

            <Route path="/customer/login" element={<CustomerLogin />} />
            <Route path="/customer/register" element={<CustomerRegister />} />
            <Route path="/customer/oauth2-callback" element={<CustomerOAuthCallback />} />
            <Route path="/customer" element={<CustomerLayout />}>
                <Route index element={<CustomerShop />} />
                <Route path="product/:id" element={<CustomerProduct />} />
                <Route path="cart" element={<CustomerCart />} />
                <Route element={<ProtectedRoute role="CUSTOMER" loginPath="/customer/login" />}>
                    <Route path="orders" element={<CustomerOrders />} />
                </Route>
            </Route>

            <Route path="/admin/login" element={<AdminLogin />} />
            <Route path="/admin" element={<AdminLayout />}>
                <Route element={<ProtectedRoute role="ADMIN" loginPath="/admin/login" />}>
                    <Route index element={<AdminDashboard />} />
                    <Route path="categories" element={<AdminCategories />} />
                    <Route path="flowers" element={<AdminFlowers />} />
                    <Route path="orders" element={<AdminOrders />} />
                    <Route path="deliveries" element={<AdminDeliveries />} />
                    <Route path="staff" element={<AdminStaff />} />
                    <Route path="reports" element={<AdminReports />} />
                </Route>
            </Route>

            <Route path="/delivery/login" element={<DeliveryLogin />} />
            <Route path="/delivery" element={<DeliveryLayout />}>
                <Route element={<ProtectedRoute role="DELIVERY" loginPath="/delivery/login" />}>
                    <Route index element={<DeliveryDeliveries />} />
                </Route>
            </Route>
        </Routes>
    );
}
