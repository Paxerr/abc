import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../store/useAuthStore';

export const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // For HTTP-only refresh token cookies
  timeout: 15000,
});

// Request Interceptor: Attach JWT Bearer Token & Log Request
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    // Structured Debug Logging
    const url = `${config.baseURL || ''}${config.url || ''}`;
    const method = (config.method || 'GET').toUpperCase();
    console.groupCollapsed(`%c[API Request] ${method} ${url}`, 'color: #0284c7; font-weight: bold;');
    console.log('Timestamp:', new Date().toISOString());
    console.log('Headers:', config.headers);
    if (config.data) {
      // Avoid logging sensitive password values directly in plain text in production
      const sanitizedData = { ...config.data };
      if (sanitizedData.password) sanitizedData.password = '******';
      console.log('Body Payload:', sanitizedData);
    }
    console.groupEnd();

    return config;
  },
  (error) => {
    console.error('[API Request Error]:', error);
    return Promise.reject(error);
  }
);

// Response Interceptor: Log Result & Handle Common Status Codes
apiClient.interceptors.response.use(
  (response) => {
    const url = response.config.url;
    const status = response.status;
    console.groupCollapsed(`%c[API Response] ${status} ${url}`, 'color: #16a34a; font-weight: bold;');
    console.log('Status Text:', response.statusText);
    console.log('Response Body:', response.data);
    console.groupEnd();

    return response;
  },
  (error: AxiosError) => {
    const status = error.response?.status;
    const url = error.config?.url;
    const responseData = error.response?.data;

    console.group(`%c[API Error] ${status || 'NETWORK_ERROR'} ${url || ''}`, 'color: #dc2626; font-weight: bold;');
    console.error('Error Message:', error.message);
    console.error('Response Data:', responseData);
    console.groupEnd();

    // Handle token expiration or unauthorized access
    if (status === 401 && !url?.includes('/auth/login')) {
      console.warn('[API Auth] Session expired or unauthorized (401). Clearing credentials...');
      useAuthStore.getState().logout();
    }

    return Promise.reject(error);
  }
);
