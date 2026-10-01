import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest, formatMoney } from "../../api/client";
import { useCart } from "../../context/CartContext";
import Alert from "../../components/Alert";

export default function CustomerShop() {
    const { addToCart } = useCart();
    const [categories, setCategories] = useState([]);
    const [flowers, setFlowers] = useState(null);
    const [search, setSearch] = useState("");
    const [category, setCategory] = useState("");
    const [error, setError] = useState("");
    const [addedId, setAddedId] = useState(null);

    useEffect(() => {
        apiRequest("/categories", { auth: false })
            .then(setCategories)
            .catch((err) => setError(err.message));
    }, []);

    async function loadFlowers(searchValue, categoryValue) {
        setError("");
        setFlowers(null);

        const params = new URLSearchParams();
        if (searchValue.trim()) params.set("search", searchValue.trim());
        if (categoryValue) params.set("category", categoryValue);
        params.set("size", "50");

        try {
            const page = await apiRequest(`/flowers?${params.toString()}`, { auth: false });
            setFlowers(page.content);
        } catch (err) {
            setError(err.message);
            setFlowers([]);
        }
    }

    useEffect(() => {
        loadFlowers("", "");
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    function handleSearchClick() {
        loadFlowers(search, category);
    }

    function handleSearchKeyDown(e) {
        if (e.key === "Enter") loadFlowers(search, category);
    }

    function handleCategoryChange(e) {
        const value = e.target.value;
        setCategory(value);
        loadFlowers(search, value);
    }

    function handleAddToCart(flower) {
        addToCart(flower, 1);
        setAddedId(flower.id);
        setTimeout(() => setAddedId((current) => (current === flower.id ? null : current)), 1000);
    }

    return (
        <>
            <section className="hero">
                <div className="hero-inner">
                    <div className="hero-copy">
                        <p className="hero-eyebrow">Flowers From the Heart</p>
                        <h1>Flowers</h1>
                        <p>
                            Fresh flowers crafted to express every emotion — from love and joy
                            to celebrations and surprises. Beautifully arranged, carefully delivered.
                        </p>
                        <a href="#shop" className="btn btn-primary">Shop now →</a>
                    </div>
                    <div className="hero-blossom" aria-hidden="true">🌸</div>
                </div>
            </section>

            <div className="feature-strip">
                <div className="feature-item">
                    <div className="feature-icon">🌷</div>
                    <strong>Fresh Blooms</strong>
                    <span>Handpicked daily</span>
                </div>
                <div className="feature-item">
                    <div className="feature-icon">🚚</div>
                    <strong>Fast Delivery</strong>
                    <span>To your door</span>
                </div>
                <div className="feature-item">
                    <div className="feature-icon">🛡️</div>
                    <strong>Trusted Florists</strong>
                    <span>Handcrafted with care</span>
                </div>
                <div className="feature-item">
                    <div className="feature-icon">🎉</div>
                    <strong>All Occasions</strong>
                    <span>Birthdays to weddings</span>
                </div>
                <div className="feature-item">
                    <div className="feature-icon">🌿</div>
                    <strong>Eco Packaging</strong>
                    <span>Kind to nature</span>
                </div>
            </div>

            <div id="shop" className="shop-heading">
                <h2>Shop Fresh Flowers</h2>
                <p>Explore a curated range of fresh, handpicked flowers, thoughtfully arranged to deliver beauty and elegance right to your doorstep.</p>
            </div>

            <div className="form form-inline mb-1">
                <div className="form-group">
                    <label htmlFor="search">Search</label>
                    <input
                        type="text"
                        id="search"
                        placeholder="e.g. rose"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        onKeyDown={handleSearchKeyDown}
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="category">Category</label>
                    <select id="category" value={category} onChange={handleCategoryChange}>
                        <option value="">All categories</option>
                        {categories.map((c) => (
                            <option key={c.id} value={c.id}>{c.name}</option>
                        ))}
                    </select>
                </div>
                <button id="searchBtn" className="btn btn-secondary" onClick={handleSearchClick}>Search</button>
            </div>

            <Alert message={error} />

            {flowers === null ? (
                <p className="muted">Loading…</p>
            ) : flowers.length === 0 ? (
                <div className="empty-state">No flowers found.</div>
            ) : (
                <div className="grid">
                    {flowers.map((flower) => (
                        <div className="card card-flower" key={flower.id}>
                            <Link to={`/customer/product/${flower.id}`}>
                                <img
                                    src={flower.imageUrl || "https://placehold.co/400x300?text=" + encodeURIComponent(flower.name)}
                                    alt={flower.name}
                                />
                                <h3>{flower.name}</h3>
                            </Link>
                            <p className="muted">{flower.categoryName}</p>
                            <p className="price">{formatMoney(flower.price)}</p>
                            <p className="muted">{flower.stockQuantity > 0 ? `${flower.stockQuantity} in stock` : "Out of stock"}</p>
                            <button
                                className="btn btn-primary btn-sm"
                                disabled={flower.stockQuantity === 0}
                                onClick={() => handleAddToCart(flower)}
                            >
                                {addedId === flower.id ? "Added ✓" : "Add to cart"}
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </>
    );
}
