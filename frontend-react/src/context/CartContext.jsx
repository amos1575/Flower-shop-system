import { createContext, useContext, useState, useCallback } from "react";

const CartContext = createContext(null);

function readCart() {
    return JSON.parse(localStorage.getItem("cart") || "[]");
}

function writeCart(cart) {
    localStorage.setItem("cart", JSON.stringify(cart));
}

export function CartProvider({ children }) {
    const [cart, setCart] = useState(readCart());

    const persist = useCallback((next) => {
        writeCart(next);
        setCart(next);
    }, []);

    const addToCart = useCallback((flower, quantity) => {
        const current = readCart();
        const existing = current.find((item) => item.flowerId === flower.id);
        if (existing) {
            existing.quantity += quantity;
        } else {
            current.push({ flowerId: flower.id, name: flower.name, price: flower.price, quantity });
        }
        persist(current);
    }, [persist]);

    const updateQuantity = useCallback((flowerId, quantity) => {
        let current = readCart();
        if (quantity <= 0) {
            current = current.filter((item) => item.flowerId !== flowerId);
        } else {
            const item = current.find((i) => i.flowerId === flowerId);
            if (item) item.quantity = quantity;
        }
        persist(current);
    }, [persist]);

    const removeFromCart = useCallback((flowerId) => {
        persist(readCart().filter((item) => item.flowerId !== flowerId));
    }, [persist]);

    const clearCart = useCallback(() => {
        localStorage.removeItem("cart");
        setCart([]);
    }, []);

    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    const total = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

    return (
        <CartContext.Provider value={{ cart, addToCart, updateQuantity, removeFromCart, clearCart, count, total }}>
            {children}
        </CartContext.Provider>
    );
}

export function useCart() {
    const ctx = useContext(CartContext);
    if (!ctx) throw new Error("useCart must be used within a CartProvider");
    return ctx;
}
