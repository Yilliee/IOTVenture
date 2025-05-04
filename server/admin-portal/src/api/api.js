// src/api/api.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://your-api-server.com/api', // Replace with your API base URL
});

// Add a request interceptor to include the auth token if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;