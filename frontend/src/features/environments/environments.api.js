import { api } from "../../shared/api/client.js";

export const environmentsApi = {
    list: (projectId) => api.get(`/projects/${projectId}/environments`),
    create: (projectId, input) => api.post(`/projects/${projectId}/environments`, input),
    update: (environmentId, input) => api.patch(`/environments/${environmentId}`, input),
    remove: (environmentId) => api.delete(`/environments/${environmentId}`),
};
