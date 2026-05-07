import { apiBase } from './http'

/**
 * 构造 SSE 聊天 URL（GET）。路径与 Spring `@RequestMapping("/ai")` + `@GetMapping("/chat")` 一致。
 */
export function buildChatStreamUrl(memoryId, message) {
  const params = new URLSearchParams({
    memoryId: String(memoryId),
    message,
  })
  const path = `/ai/chat?${params.toString()}`
  if (apiBase.startsWith('http')) {
    return `${apiBase}${path}`
  }
  return `${typeof window !== 'undefined' ? window.location.origin : ''}${apiBase}${path}`
}
