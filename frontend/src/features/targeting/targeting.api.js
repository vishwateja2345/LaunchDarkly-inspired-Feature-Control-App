import { api } from "../../shared/api/client.js";

export const segmentsApi = {
    list: (projectId) => api.get(`/projects/${projectId}/segments`),
    get: (segmentId) => api.get(`/segments/${segmentId}`),
    create: (projectId, input) => api.post(`/projects/${projectId}/segments`, input),
    update: (segmentId, input) => api.put(`/segments/${segmentId}`, input),
    remove: (segmentId) => api.delete(`/segments/${segmentId}`),
    preview: (segmentId) => api.get(`/segments/${segmentId}/preview`),
};

export const appUsersApi = {
    list: (projectId, search) => api.get(`/projects/${projectId}/users${search ? `?search=${encodeURIComponent(search)}` : ""}`),
    create: (projectId, input) => api.post(`/projects/${projectId}/users`, input),
    update: (userId, input) => api.patch(`/users/${userId}`, input),
    remove: (userId) => api.delete(`/users/${userId}`),
};
