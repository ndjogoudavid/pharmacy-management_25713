import axios from 'axios';
import { getToken } from './utils/token';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response } = error || {};

    if (response) {
      const data = response.data;
      // Attach a normalized userMessage if backend sent structured ApiError
      if (data && typeof data === 'object') {
        const message =
          data.message ||
          data.error ||
          (typeof data === 'string' ? data : 'Request failed');
        // eslint-disable-next-line no-param-reassign
        error.userMessage = message;
        // Normalize field-level validation errors to a stable shape
        // so forms can always call error.response.data.fieldErrors safely
        // (backend may use different shapes; we prefer data.fieldErrors)
        // eslint-disable-next-line no-param-reassign
        if (!data.fieldErrors && data.errors) {
          // Try common alternate naming
          // eslint-disable-next-line no-param-reassign
          data.fieldErrors = data.errors;
        }
      }

      if (response.status === 401) {
        try {
          // Friendly UX: show a short alert before redirecting to login
          // If the app later integrates a toast system, replace this alert with it.
          if (window.location.pathname !== '/login') {
            // show message then clear local state and redirect
            // eslint-disable-next-line no-alert
            alert('Your session has expired. Please sign in again.');
          }
        } catch (e) {
          // ignore alert errors
        }
        // Clear auth and force re-login when backend says "unauthorized"
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('email');
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }

    return Promise.reject(error);
  }
);

export default api;
