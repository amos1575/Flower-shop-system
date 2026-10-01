import { createContext, useContext, useState, useCallback } from "react";
import {
    apiRequest, getToken, setToken, clearToken,
    getStoredUser, setStoredUser, clearStoredUser,
} from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(getStoredUser());

    // expectedRole: if provided, a login whose response.role doesn't match throws
    // and nothing is persisted (keeps a portal's login page from leaving another
    // role's session lingering in localStorage).
    const login = useCallback(async (email, password, expectedRole) => {
        const response = await apiRequest("/auth/login", { method: "POST", body: { email, password }, auth: false });
        if (expectedRole && response.role !== expectedRole) {
            throw new Error(`This login is for ${expectedRole === "ADMIN" ? "administrators" : expectedRole === "DELIVERY" ? "delivery personnel" : "customers"} only.`);
        }
        setToken(response.token);
        setStoredUser(response);
        setUser(response);
        return response;
    }, []);

    const register = useCallback(async (body) => {
        const response = await apiRequest("/auth/register", { method: "POST", body, auth: false });
        setToken(response.token);
        setStoredUser(response);
        setUser(response);
        return response;
    }, []);

    const logout = useCallback(() => {
        clearToken();
        clearStoredUser();
        setUser(null);
    }, []);

    return (
        <AuthContext.Provider value={{ user, token: getToken(), login, register, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
    return ctx;
}
