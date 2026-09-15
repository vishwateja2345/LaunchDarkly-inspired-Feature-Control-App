import { api } from "../../shared/api/client.js";

export const authApi = {
    login: (email, password) => api.post("/auth/login", { email, password }),
    session: () => api.get("/auth/session"),
    logout: () => api.post("/auth/logout"),
};
