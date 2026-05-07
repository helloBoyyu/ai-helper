<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { buildChatStreamUrl } from './api/chat'
import { renderMarkdownToHtml } from './utils/markdown'

/** Java int 范围内的会话 ID（memoryId） */
function randomMemoryId() {
  return Math.floor(Math.random() * 2147483647)
}

const memoryId = ref(randomMemoryId())
const input = ref('')
const messages = ref([])
const streaming = ref(false)
const chatBodyRef = ref(null)
/** 中文等 IME 组字过程中为 true，避免选词/上屏时的 Enter 误触发发送 */
const imeActive = ref(false)
/** 用户点击「终止」关闭 SSE 时置位，避免 onerror 误填「未能收到回复」 */
const pendingUserAbort = ref(false)

let es = null

/** 回复中主按钮为终止，否则无内容时禁用发送 */
const composerPrimaryDisabled = computed(
  () => !streaming.value && input.value.trim().length === 0,
)

function scrollToBottom() {
  nextTick(() => {
    const el = chatBodyRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

watch(messages, () => scrollToBottom(), { deep: true })

function stopStream() {
  if (es) {
    es.close()
    es = null
  }
}

function appendAssistantChunk(chunk) {
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant') {
    last.content += chunk
  }
}

function sendMessage() {
  const text = input.value.trim()
  if (!text || streaming.value) return

  pendingUserAbort.value = false

  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '', streaming: true })
  input.value = ''
  streaming.value = true

  stopStream()
  const url = buildChatStreamUrl(memoryId.value, text)

  es = new EventSource(url)

  es.onmessage = (event) => {
    appendAssistantChunk(event.data ?? '')
  }

  es.onerror = () => {
    const userStopped = pendingUserAbort.value
    pendingUserAbort.value = false
    stopStream()
    streaming.value = false
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      delete last.streaming
      if (!userStopped && !last.content.trim()) {
        last.content =
          '未能收到回复内容。请确认后端已启动，且存在 GET /api/ai/chat；本地开发可执行 npm run dev 使用代理避免跨域。'
      }
    }
  }
}

function abortReply() {
  pendingUserAbort.value = true
  stopStream()
  streaming.value = false
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant') {
    delete last.streaming
  }
}

function handleSend() {
  sendMessage()
}

function onComposerPrimaryClick() {
  if (streaming.value) {
    abortReply()
  } else {
    handleSend()
  }
}

function onImeCompositionStart() {
  imeActive.value = true
}

function onImeCompositionEnd() {
  imeActive.value = false
}

function onKeydown(e) {
  if (e.key !== 'Enter' || e.shiftKey) return
  // IME 组字或选词：Enter 用于确认，不应发送（229 为部分浏览器在 IME 处理时的 keyCode）
  if (e.isComposing || imeActive.value || e.keyCode === 229) return
  e.preventDefault()
  onComposerPrimaryClick()
}

function newSession() {
  pendingUserAbort.value = false
  stopStream()
  streaming.value = false
  memoryId.value = randomMemoryId()
  messages.value = []
}

onMounted(() => {
  scrollToBottom()
})

onUnmounted(() => {
  stopStream()
})
</script>

<template>
  <div class="app">
    <header class="header">
      <div class="title-block">
        <h1 class="title">AI 小助手</h1>
        <p class="subtitle">编程学习 · 面试求职 · 随时提问</p>
      </div>
      <div class="session-bar">
        <span class="session-label">会话 ID</span>
        <code class="session-id">{{ memoryId }}</code>
        <button type="button" class="btn-ghost" :disabled="streaming" @click="newSession">
          新会话
        </button>
      </div>
    </header>

    <main class="chat-shell">
      <div ref="chatBodyRef" class="chat-body">
        <div v-if="messages.length === 0" class="empty-hint">
          <p>你好，我是 AI 小助手。</p>
          <p>在下方输入编程或面试相关的问题，我会通过流式回复给出建议。</p>
        </div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="msg-row"
          :class="msg.role === 'user' ? 'msg-row--user' : 'msg-row--ai'"
        >
          <div class="bubble" :class="msg.role === 'user' ? 'bubble--user' : 'bubble--ai'">
            <span class="bubble-label">{{ msg.role === 'user' ? '我' : 'AI' }}</span>
            <div
              v-if="msg.role === 'assistant'"
              class="bubble-text markdown-body"
              v-html="renderMarkdownToHtml(msg.content)"
            ></div>
            <div v-else class="bubble-text bubble-text--plain">{{ msg.content }}</div>
            <span v-if="msg.streaming" class="cursor-blink" aria-hidden="true">▍</span>
          </div>
        </div>
      </div>

      <footer class="composer">
        <textarea
          v-model="input"
          class="input"
          rows="2"
          placeholder="输入你的问题…（Enter 发送，Shift+Enter 换行）"
          :disabled="streaming"
          @compositionstart="onImeCompositionStart"
          @compositionend="onImeCompositionEnd"
          @keydown="onKeydown"
        />
        <button
          type="button"
          class="btn-send"
          :class="{ 'btn-send--stop': streaming }"
          :disabled="composerPrimaryDisabled"
          @click="onComposerPrimaryClick"
        >
          {{ streaming ? '终止' : '发送' }}
        </button>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(165deg, #0f1419 0%, #1a2332 45%, #121820 100%);
  color: #e8eaed;
}

.header {
  flex-shrink: 0;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 0.75rem;
}

.title-block .title {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.85rem;
  color: rgba(232, 234, 237, 0.55);
}

.session-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.session-label {
  font-size: 0.75rem;
  color: rgba(232, 234, 237, 0.45);
}

.session-id {
  font-size: 0.8rem;
  padding: 0.2rem 0.5rem;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.06);
  color: #a8c7fa;
}

.btn-ghost {
  font-size: 0.8rem;
  padding: 0.35rem 0.65rem;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: transparent;
  color: #e8eaed;
  cursor: pointer;
}

.btn-ghost:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.06);
}

.btn-ghost:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.chat-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-width: 880px;
  width: 100%;
  margin: 0 auto;
  padding: 0.75rem 1rem 1.25rem;
  box-sizing: border-box;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.empty-hint {
  text-align: center;
  padding: 2rem 1rem;
  color: rgba(232, 234, 237, 0.5);
  font-size: 0.95rem;
  line-height: 1.6;
}

.empty-hint p {
  margin: 0.35rem 0;
}

.msg-row {
  display: flex;
  width: 100%;
}

.msg-row--user {
  justify-content: flex-end;
}

.msg-row--ai {
  justify-content: flex-start;
}

.bubble {
  max-width: min(85%, 560px);
  padding: 0.65rem 0.9rem;
  border-radius: 14px;
  position: relative;
  line-height: 1.55;
  font-size: 0.95rem;
}

.bubble-label {
  display: block;
  font-size: 0.7rem;
  margin-bottom: 0.35rem;
  opacity: 0.55;
  font-weight: 500;
}

.bubble-text {
  word-break: break-word;
}

.bubble-text--plain {
  white-space: pre-wrap;
}

/* Markdown（AI） */
.markdown-body :deep(p) {
  margin: 0 0 0.65em;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 0.85em 0 0.45em;
  font-weight: 600;
  line-height: 1.35;
  color: #e8eaed;
}

.markdown-body :deep(h1) {
  font-size: 1.25rem;
}
.markdown-body :deep(h2) {
  font-size: 1.1rem;
}
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  font-size: 1rem;
}

.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child) {
  margin-top: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.35em 0 0.65em;
  padding-left: 1.35rem;
}

.markdown-body :deep(li) {
  margin: 0.2em 0;
}

.markdown-body :deep(blockquote) {
  margin: 0.5em 0;
  padding: 0.35rem 0.65rem;
  border-left: 3px solid rgba(168, 199, 250, 0.45);
  background: rgba(0, 0, 0, 0.2);
  color: rgba(232, 234, 237, 0.88);
}

.markdown-body :deep(pre) {
  margin: 0.5em 0;
  padding: 0.65rem 0.75rem;
  border-radius: 8px;
  overflow-x: auto;
  background: rgba(0, 0, 0, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 0.85rem;
  line-height: 1.5;
}

.markdown-body :deep(code) {
  font-family: ui-monospace, 'Cascadia Code', 'Source Code Pro', Menlo, Consolas, monospace;
  font-size: 0.88em;
}

.markdown-body :deep(p > code),
.markdown-body :deep(li > code) {
  padding: 0.1em 0.35em;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.35);
  color: #c4e0a4;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: none;
  color: #d4d4d4;
  font-size: inherit;
}

.markdown-body :deep(a) {
  color: #8ab4f8;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.markdown-body :deep(a:hover) {
  color: #a8c7fa;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
  margin: 0.5em 0;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 0.35rem 0.5rem;
  text-align: left;
}

.markdown-body :deep(th) {
  background: rgba(0, 0, 0, 0.25);
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  margin: 0.75em 0;
}

.bubble--user {
  background: linear-gradient(135deg, #394867 0%, #2d3a52 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.bubble--ai {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(168, 199, 250, 0.15);
}

.cursor-blink {
  display: inline-block;
  animation: blink 1s step-end infinite;
  margin-left: 1px;
  color: #a8c7fa;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.composer {
  flex-shrink: 0;
  display: flex;
  gap: 0.65rem;
  align-items: flex-end;
  padding-top: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.input {
  flex: 1;
  resize: none;
  padding: 0.65rem 0.85rem;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(0, 0, 0, 0.25);
  color: #e8eaed;
  font-family: inherit;
  font-size: 0.95rem;
  line-height: 1.45;
}

.input:focus {
  outline: none;
  border-color: rgba(168, 199, 250, 0.45);
  box-shadow: 0 0 0 2px rgba(168, 199, 250, 0.12);
}

.input:disabled {
  opacity: 0.55;
}

.btn-send {
  flex-shrink: 0;
  padding: 0.65rem 1.1rem;
  border-radius: 12px;
  border: none;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  background: linear-gradient(135deg, #8ab4f8 0%, #669df6 100%);
  color: #0f1419;
}

.btn-send:hover:not(:disabled) {
  filter: brightness(1.05);
}

.btn-send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.btn-send--stop {
  background: linear-gradient(135deg, #f6aea9 0%, #e57373 100%);
  color: #1a0f0f;
}

.btn-send--stop:hover:not(:disabled) {
  filter: brightness(1.06);
}
</style>
