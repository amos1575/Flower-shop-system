import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { apiRequest, formatMoney } from "../../api/client";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";
import Alert from "../../components/Alert";

export default function CustomerCart() {
    const { user } = useAuth();
    const { cart, updateQuantity, removeFromCart, clearCart, total } = useCart();
    const navigate = useNavigate();

    const [deliveryAddress, setDeliveryAddress] = useState(user?.address || "");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    if (cart.length === 0) {
        return (
            <>
                <h1>Your Cart</h1>
                <div className="empty-state">Your cart is empty. <Link to="/customer">Browse flowers</Link>.</div>
            </>
        );
    }

    async function handleCheckout() {
        setError("");
        const address = deliveryAddress.trim();
        if (!address) {
            setError("Please enter a delivery address.");
            return;
        }
        setSubmitting(true);
        try {
            const order = await apiRequest("/orders", {
                method: "POST",
                body: {
                    deliveryAddress: address,
                    items: cart.map((item) => ({ flowerId: item.flowerId, quantity: item.quantity })),
                },
            });
            clearCart();
            navigate(`/customer/orders?placed=${order.id}`);
        } catch (err) {
            setError(err.message);
            setSubmitting(false);
        }
    }

    return (
        <>
            <h1>Your Cart</h1>
            <Alert message={error} />
            <div className="table-wrap">
                <table>
                    <thead>
                        <tr><th>Flower</th><th>Price</th><th>Qty</th><th>Subtotal</th><th></th></tr>
                    </thead>
                    <tbody>
                        {cart.map((item) => (
                            <tr key={item.flowerId}>
                                <td>{item.name}</td>
                                <td>{formatMoney(item.price)}</td>
                                <td>
                                    <input
                                        type="number"
                                        min={1}
                                        value={item.quantity}
                                        style={{ width: "60px" }}
                                        onChange={(e) => updateQuantity(item.flowerId, Number(e.target.value))}
                                    />
                                </td>
                                <td>{formatMoney(item.price * item.quantity)}</td>
                                <td>
                                    <button className="btn btn-sm btn-danger" onClick={() => removeFromCart(item.flowerId)}>Remove</button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <p className="mt-1"><strong>Total: {formatMoney(total)}</strong></p>

            {user ? (
                <div className="form mt-1">
                    <div className="form-group">
                        <label htmlFor="deliveryAddress">Delivery address</label>
                        <input
                            type="text"
                            id="deliveryAddress"
                            placeholder="Street, City"
                            value={deliveryAddress}
                            onChange={(e) => setDeliveryAddress(e.target.value)}
                        />
                    </div>
                    <button className="btn btn-primary" disabled={submitting} onClick={handleCheckout}>Place order</button>
                </div>
            ) : (
                <p className="mt-1"><Link to="/customer/login">Log in</Link> to check out.</p>
            )}
        </>
    );
}
