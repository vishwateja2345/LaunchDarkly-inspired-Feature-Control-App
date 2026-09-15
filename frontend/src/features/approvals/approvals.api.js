import { api } from "../../shared/api/client.js";

export const approvalsApi = {
    list: (projectId, status) => api.get(`/approvals?projectId=${projectId}${status ? `&status=${status}` : ""}`),
    get: (approvalId) => api.get(`/approvals/${approvalId}`),
    approve: (approvalId, input) => api.post(`/approvals/${approvalId}/approve`, input),
    reject: (approvalId, input) => api.post(`/approvals/${approvalId}/reject`, input),
    cancel: (approvalId) => api.post(`/approvals/${approvalId}/cancel`),
};
