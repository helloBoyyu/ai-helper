import axios from 'axios'

/**
 * 后端前缀：开发环境可走 Vite 代理（相对路径 /api），生产可配置 VITE_API_BASE。
 */
const rawBase = import.meta.env.VITE_API_BASE?.trim() || '/api'

export const apiBase = rawBase.endsWith('/') ? rawBase.slice(0, -1) : rawBase

export const http = axios.create({
  baseURL: apiBase,
  timeout: 60000,
})
