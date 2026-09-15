import { api } from "../../shared/api/client.js";

export const projectsApi = {
    list: () => api.get("/projects"),
    get: (projectId) => api.get(`/projects/${projectId}`),
    create: (input) => api.post("/projects", input),
    update: (projectId, input) => api.patch(`/projects/${projectId}`, input),
    remove: (projectId) => api.delete(`/projects/${projectId}`),
};
