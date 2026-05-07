import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.use({
  gfm: true,
  breaks: true,
})

let hooksInstalled = false

function installDomPurifyHooks() {
  if (hooksInstalled) return
  hooksInstalled = true
  DOMPurify.addHook('afterSanitizeAttributes', (node) => {
    if (node.tagName === 'A') {
      const href = node.getAttribute('href')
      if (href && /^https?:\/\//i.test(href)) {
        node.setAttribute('target', '_blank')
        node.setAttribute('rel', 'noopener noreferrer')
      }
    }
  })
}

installDomPurifyHooks()

/**
 * 将 Markdown 转为可安全插入页面的 HTML（AI 回复）。
 */
export function renderMarkdownToHtml(markdown) {
  const src = markdown ?? ''
  const raw = marked.parse(src, { async: false })
  if (typeof raw !== 'string') return ''
  return DOMPurify.sanitize(raw)
}
