import axios from 'axios'
import { apiBaseUrl } from '@/config/api'

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 5000
})

// Request interceptor (can be left as-is for now)
request.interceptors.request.use(config => {
  return config
})

// Response interceptor
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
