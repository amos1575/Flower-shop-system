import { useEffect, useState, useCallback } from "react";
import { useParams, Link } from "react-router-dom";
import { apiRequest, formatMoney, formatDate } from "../../api/client";
import { useAuth } from "../../context/AuthContext";
import { useCart } from "../../context/CartContext";
import Alert from "../../components/Alert";

function starString(rating) {
    const rounded = Math.round(rating);
    return "★".repeat(rounded) + "☆".repeat(5 - rounded);
}

export default function CustomerProduct() {
    const { id: flowerId } = useParams();
    const { user } = useAuth();
    const { addToCart } = useCart();

    const [flower, setFlower] = useState(null);
    const [error, setError] = useState("");
    const [qty, setQty] = useState(1);
    const [added, setAdded] = useState(false);

    const [summary, setSummary] = useState(null);
    const [reviewAlert, setReviewAlert] = useState({ message: "", type: "success" });
    const [rating, setRating] = useState("5");
    const [comment, setComment] = useState("");

    const loadProduct = useCallback(async () => {
        try {
            const data = await apiRequest(`/flowers/${flowerId}`, { auth: false });
            document.title = `${data.name} — Flower Shop`;
            setFlower(data);
            setQty(1);
        } catch (err) {
            setError(err.message);
        }
    }, [flowerId]);

    const loadReviews = useCallback(async () => {
        try {
            const data = await apiRequest(`/flowers/${flowerId}/reviews?size=20`, { auth: false });
            setSummary(data);
        } catch (err) {
            setError(err.message);
        }
    }, [flowerId]);

    useEffect(() => {
        loadProduct();
        loadReviews();
    }, [loadProduct, loadReviews]);

    function handleAddToCart() {
        const quantity = Math.max(1, Number(qty) || 1);
        addToCart(flower, quantity);
        setAdded(true);
        setTimeout(() => setAdded(false), 1000);
    }

    async function handleReviewSubmit(e) {
        e.preventDefault();
        setReviewAlert({ message: "", type: "success" });
        try {
            await apiRequest(`/flowers/${flowerId}/reviews`, {
                method: "POST",
                body: { rating: Number(rating), comment: comment.trim() || null },
            });
            setReviewAlert({ message: "Review submitted!", type: "success" });
            setComment("");
            loadReviews();
        } catch (err) {
            setReviewAlert({ message: err.message, type: "error" });
        }
    }

    return (
        <>
            <p><Link to="/customer">&larr; Back to shop</Link></p>
            <Alert message={error} />

            {flower && (
                <div className="product-layout">
                    <img
                        src={flower.imageUrl || "https://placehold.co/400x300?text=" + encodeURIComponent(flower.name)}
                        alt={flower.name}
                    />
                    <div className="product-info">
                        <h1>{flower.name}</h1>
                        <p className="muted">{flower.categoryName}</p>
                        <p className="price">{formatMoney(flower.price)}</p>
                        <p>{flower.description || ""}</p>
                        <p className="muted">{flower.stockQuantity > 0 ? `${flower.stockQuantity} in stock` : "Out of stock"}</p>
                        <div className="flex mt-1">
                            <input
                                type="number"
                                id="qty"
                                value={qty}
                                min={1}
                                max={flower.stockQuantity}
                                style={{ width: "70px" }}
                                className="form-group"
                                onChange={(e) => setQty(e.target.value)}
                            />
                            <button className="btn btn-primary" disabled={flower.stockQuantity === 0} onClick={handleAddToCart}>
                                {added ? "Added ✓" : "Add to cart"}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <h2 className="mt-2">Reviews</h2>
            <div className="muted mb-1">
                {summary && (
                    <>
                        <span className="stars">{starString(summary.averageRating)}</span>{" "}
                        {summary.averageRating.toFixed(1)} ({summary.reviewCount} review{summary.reviewCount === 1 ? "" : "s"})
                    </>
                )}
            </div>
            <Alert message={reviewAlert.message} type={reviewAlert.type} />

            {user ? (
                <form className="form form-inline mb-1" onSubmit={handleReviewSubmit}>
                    <div className="form-group">
                        <label htmlFor="rating">Rating</label>
                        <select id="rating" value={rating} onChange={(e) => setRating(e.target.value)}>
                            <option value="5">5 — Excellent</option>
                            <option value="4">4 — Good</option>
                            <option value="3">3 — Average</option>
                            <option value="2">2 — Poor</option>
                            <option value="1">1 — Terrible</option>
                        </select>
                    </div>
                    <div className="form-group" style={{ flex: 2 }}>
                        <label htmlFor="comment">Comment</label>
                        <input type="text" id="comment" placeholder="Optional" value={comment} onChange={(e) => setComment(e.target.value)} />
                    </div>
                    <button type="submit" className="btn btn-secondary">Submit review</button>
                </form>
            ) : (
                <p className="muted mb-1"><Link to="/customer/login">Log in</Link> to leave a review.</p>
            )}

            {summary && (
                summary.reviews.content.length === 0 ? (
                    <p className="muted">No reviews yet.</p>
                ) : (
                    summary.reviews.content.map((r) => (
                        <div className="review-item" key={r.id}>
                            <div className="flex-between">
                                <strong>{r.customerName}</strong>
                                <span className="stars">{starString(r.rating)}</span>
                            </div>
                            {r.comment && <p>{r.comment}</p>}
                            <p className="muted">{formatDate(r.createdAt)}</p>
                        </div>
                    ))
                )
            )}
        </>
    );
}
