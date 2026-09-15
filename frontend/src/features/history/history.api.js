import { api } from "../../shared/api/client.js";

export const historyApi = {
    listForFlag: (flagId) => api.get(`/flags/${flagId}/history`),
    listForProject: (projectId, limit = 50) => api.get(`/projects/${projectId}/history?limit=${limit}`),
    get: (entryId) => api.get(`/history/${entryId}`),
};
