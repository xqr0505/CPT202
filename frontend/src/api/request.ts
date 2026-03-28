import axios from 'axios';
import { ElMessage } from 'element-plus';

const service = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  timeout: 10000
});

service.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

service.interceptors.response.use(
  response => {
    const res = response.data;
    if (res.code !== 200) {
      ElMessage.error(res.message || 'Error');
      return Promise.reject(new Error(res.message));
    }
    return res.data;
  },
  error => {
    if (error.response?.status === 401) {
      // TODO: Clear token and redirect to login page
    }
    return Promise.reject(error);
  }
);
export default service;
