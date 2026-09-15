import { api } from "../../shared/api/client.js";

export const flagsApi = {
    list: (projectId, { includeArchived = false } = {}) =>
        api.get(`/projects/${projectId}/flags?includeArchived=${includeArchived}`),
    get: (flagId) => api.get(`/flags/${flagId}`),
    create: (projectId, input) => api.post(`/projects/${projectId}/flags`, input),
    update: (flagId, input) => api.patch(`/flags/${flagId}`, input),
    archive: (flagId) => api.post(`/flags/${flagId}/archive`),
    restore: (flagId) => api.post(`/flags/${flagId}/restore`),

    getConfig: (flagId, environmentKey) => api.get(`/flags/${flagId}/environments/${environmentKey}/config`),
    toggle: (flagId, environmentKey, enabled) =>
        api.post(`/flags/${flagId}/environments/${environmentKey}/toggle`, { enabled }),
    submitChange: (flagId, environmentKey, change) =>
        api.post(`/flags/${flagId}/environments/${environmentKey}/changes`, change),
    restoreConfig: (flagId, environmentKey, historyEntryId) =>
        api.post(`/flags/${flagId}/environments/${environmentKey}/restore`, { historyEntryId }),

    evaluate: (input) => api.post("/evaluate", input),
};
