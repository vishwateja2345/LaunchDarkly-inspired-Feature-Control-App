import { api } from "../../shared/api/client.js";

export const experimentsApi = {
    metricKeys: (flagId, environmentKey) => api.get(`/flags/${flagId}/environments/${environmentKey}/experiment/metrics`),
    compare: (flagId, environmentKey, metricKey) =>
        api.get(`/flags/${flagId}/environments/${environmentKey}/experiment${metricKey ? `?metricKey=${encodeURIComponent(metricKey)}` : ""}`),
    recordExposure: (flagId, environmentKey, input) =>
        api.post(`/flags/${flagId}/environments/${environmentKey}/events/exposure`, input),
    recordConversion: (flagId, environmentKey, input) =>
        api.post(`/flags/${flagId}/environments/${environmentKey}/events/conversion`, input),
};
