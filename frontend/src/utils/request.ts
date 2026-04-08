import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081',
  timeout: 5000
})

// 请求拦截器（可以先不用管）
request.interceptors.request.use(config => {
  return config
})

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

export default request
