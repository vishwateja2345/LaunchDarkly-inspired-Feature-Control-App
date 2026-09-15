import { api } from "../../shared/api/client.js";

export const accountsApi = {
    list: () => api.get("/accounts"),
};
