const API_ORIGIN = "http://localhost:8080";
const API_BASE_URL = `${API_ORIGIN}/api`;

export const GOOGLE_LOGIN_URL = `${API_ORIGIN}/oauth2/authorization/google`;

export function getToken() {
    return localStorage.getItem("jwt");
}

export function setToken(token) {
    localStorage.setItem("jwt", token);
}

export function clearToken() {
    localStorage.removeItem("jwt");
}

export function getStoredUser() {
    const raw = localStorage.getItem("user");
    return raw ? JSON.parse(raw) : null;
}

export function setStoredUser(user) {
    localStorage.setItem("user", JSON.stringify(user));
}

export function clearStoredUser() {
    localStorage.removeItem("user");
}

export async function apiRequest(path, { method = "GET", body, auth = true } = {}) {
    const headers = { "Content-Type": "application/json" };
    const token = getToken();
    if (auth && token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(errorBody.message || `Request failed with status ${response.status}`);
    }

    if (response.status === 204) return null;
    return response.json();
}

export function formatMoney(amount) {
    return `$${Number(amount).toFixed(2)}`;
}

export function formatDate(isoString) {
    if (!isoString) return "—";
    return new Date(isoString).toLocaleString(undefined, {
        year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit",
    });
}
