<template>
  <div class="rag-chat-page">
    <div class="page-header">
      <div>
        <h2>AI 问答</h2>
        <p>先手动选知识库，再开始问答。不会默认替你全选。</p>
      </div>
      <div class="header-stats">
        <div class="stat-card">
          <strong>{{ knowledgeFiles.length }}</strong>
          <span>可选文件</span>
        </div>
        <div class="stat-card">
          <strong>{{ selectedFiles.length }}</strong>
          <span>已选文件</span>
        </div>
      </div>
    </div>

    <div class="chat-layout">
      <section class="chat-panel">
        <div class="source-panel">
          <div class="source-panel-head">
            <div>
              <strong>知识库选择</strong>
              <span>支持手动多选，默认不强制全选</span>
            </div>
            <div class="source-actions">
              <el-button class="source-action-btn select-all-btn" :disabled="knowledgeFiles.length === 0" @click="selectAllFiles">
                <el-icon><Check /></el-icon>
                <span>全选</span>
              </el-button>
              <el-button class="source-action-btn clear-btn" :disabled="selectedFiles.length === 0" @click="clearSelectedFiles">
                <el-icon><Close /></el-icon>
                <span>清空</span>
              </el-button>
            </div>
          </div>

          <el-select
            v-model="selectedFiles"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择文件"
            class="source-select"
            @change="handleFileSelectionChange"
          >
            <el-option
              v-for="file in knowledgeFiles"
              :key="file.id"
              :label="file.fileName"
              :value="file.id"
            />
          </el-select>

          <div v-if="selectedFiles.length > 0" class="selected-files-popover">
            <el-tag
              v-for="fileId in selectedFiles"
              :key="fileId"
              :title="getFileNameById(fileId)"
              size="small"
              closable
              @close="removeSelectedFile(fileId)"
            >
              {{ getFileNameByLength(fileId, 16) }}
            </el-tag>
          </div>
          <div v-else class="selected-files-empty">
            先选文件，再发送问题。
          </div>
        </div>

        <div class="chat-messages" ref="messageContainer">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="['message', message.role === 'user' ? 'user-message' : 'assistant-message']"
          >
            <div class="message-wrapper">
              <div class="message-avatar">
                <div v-if="message.role === 'user'" class="avatar user-avatar">
                  <el-icon><User /></el-icon>
                </div>
                <div v-else class="avatar assistant-avatar">
                  <el-icon><MagicStick /></el-icon>
                </div>
              </div>
              <div
                class="message-content"
                :class="{ typing: message.isTyping }"
                v-html="message.htmlContent || renderMarkdown(message.content)"
              />
              <el-button class="copy-button" text size="small" @click="copyMessage(message.content)">
                <el-icon><Document /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="input-container">
            <el-input
              v-model="userInput"
              type="textarea"
              :rows="3"
              placeholder="请输入您的问题，按 Enter 发送..."
              @keyup.enter="handleRagSend"
              resize="none"
            />
            <div class="button-group">
              <el-button type="primary" class="send-btn" @click="handleRagSend" :loading="isLoading">
                <el-icon v-if="!isLoading"><Position /></el-icon>
                <span>{{ isLoading ? '思考中...' : '发送' }}</span>
              </el-button>
              <el-button @click="clearMessages">清空对话</el-button>
            </div>
          </div>
        </div>
      </section>

      <aside class="inspector-panel">
        <div class="inspector-card">
          <div class="inspector-title">当前上下文</div>
          <div class="inspector-grid">
            <div>
              <strong>{{ knowledgeFiles.length }}</strong>
              <span>知识文件</span>
            </div>
            <div>
              <strong>{{ selectedFiles.length }}</strong>
              <span>当前选中</span>
            </div>
          </div>
        </div>

        <div class="inspector-card">
          <div class="inspector-title">文件列表</div>
          <div class="inspector-list">
            <div v-for="file in knowledgeFiles" :key="file.id" class="inspector-list-item">
              <span>{{ file.fileName }}</span>
              <el-tag size="small" type="info">{{ selectedFiles.includes(file.id) ? '已选' : '可选' }}</el-tag>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { marked } from 'marked'
import { Check, Close, Document, User, MagicStick, Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { ChatApi, type ChatMessage } from '@/api/ChatApi'
import { getStreamChat } from '@/api/StreamApi'
import { queryFileApi } from '@/api/KnowHubApi'
import { StoreFile } from '@/api/data'


const messages = ref<ChatMessage[]>([])
const userInput = ref('')
const isLoading = ref(false)
const messageContainer = ref<HTMLElement | null>(null)
const knowledgeFiles = ref<StoreFile[]>([])
const selectedFiles = ref<number[]>([])
const STREAM_FLUSH_INTERVAL = 80

const createMessage = (role: ChatMessage['role'], content: string, isTyping = false): ChatMessage => ({
  role,
  content,
  htmlContent: renderMarkdown(content),
  isTyping
})

// 加载知识库文件列表
const loadKnowledgeFiles = () => {
  const params = {
    page: 0,
    pageSize: 100,
    fileName: ""
  }

  queryFileApi(params)
    .then((res) => {
      if (res.code == 0) {
        const data = res.data;
        knowledgeFiles.value = data.records || [];
      } else {
        ElMessage({
          type: 'error',
          message: res.message,
        });
      }
    })
    .catch((err) => {
      ElMessage({
        type: 'error',
        message: err,
      });
    });
};

// 处理RAG对话
const handleRagSend = async () => {
  if (!userInput.value.trim() || isLoading.value) return
  await sendMessage(ChatApi.RagChat, selectedFiles.value)
}

// 发送消息通用方法
const sendMessage = async (url: string, selectedFileIds: number[] = []) => {
  messages.value.push({
    role: 'user',
    content: userInput.value,
    htmlContent: renderMarkdown(userInput.value)
  })

  const currentInput = userInput.value
  userInput.value = ''
  isLoading.value = true

  messages.value.push({
    role: 'assistant',
    content: '正在思考中...',
    htmlContent: renderMarkdown('正在思考中...'),
    isTyping: true
  })

  const lastIndex = messages.value.length - 1
  const reactiveMessage = messages.value[lastIndex]

  let isFirstChunk = true
  let pendingText = ''
  let flushTimer: number | null = null

  const flushPendingText = () => {
    if (!pendingText) return

    if (isFirstChunk && reactiveMessage.content === '正在思考中...') {
      reactiveMessage.content = ''
      isFirstChunk = false
    }

    reactiveMessage.content += pendingText
    pendingText = ''
    reactiveMessage.htmlContent = renderMarkdown(reactiveMessage.content)

    scrollToBottom()
  }

  const scheduleFlush = () => {
    if (flushTimer !== null) return

    flushTimer = window.setTimeout(() => {
      flushTimer = null
      flushPendingText()
    }, STREAM_FLUSH_INTERVAL)
  }

  const handleStreamMessage = (value: MessageEvent) => {
    pendingText += value.data || ''
    scheduleFlush()
  }

  const handleStreamClose = () => {
    if (flushTimer !== null) {
      window.clearTimeout(flushTimer)
      flushTimer = null
    }

    flushPendingText()
    isLoading.value = false
    reactiveMessage.isTyping = false
    reactiveMessage.htmlContent = renderMarkdown(reactiveMessage.content)
    scrollToBottom()
  }

  const handleStreamError = (error: unknown) => {
    window.console.error('Error:', error)

    if (flushTimer !== null) {
      window.clearTimeout(flushTimer)
      flushTimer = null
    }

    pendingText = ''
    isLoading.value = false
    reactiveMessage.isTyping = false
    reactiveMessage.content = '抱歉，发生了错误，请稍后重试。'
    reactiveMessage.htmlContent = renderMarkdown(reactiveMessage.content)
    scrollToBottom()
  }

  const fileSources = selectedFileIds.map(id => {
    const file = knowledgeFiles.value.find(f => f.id === id)
    return file ? file.fileName : ''
  }).filter(name => name !== '')

  if (fileSources.length > 0) {
    getStreamChat(currentInput, url, handleStreamMessage, handleStreamError, handleStreamClose, fileSources)
  } else {
    getStreamChat(currentInput, url, handleStreamMessage, handleStreamError, handleStreamClose)
  }
};

// 滚动到底部
const scrollToBottom = () => {
  if (!messageContainer.value) return

  const container = messageContainer.value

  requestAnimationFrame(() => {
    container.scrollTop = container.scrollHeight
  })
}

// 复制消息
const copyMessage = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage({
      message: '复制成功',
      type: 'success',
      duration: 2000
    })
  } catch (err) {
    ElMessage({
      message: '复制失败',
      type: 'error',
      duration: 2000
    })
  }
}

// 清空对话
const clearMessages = () => {
  messages.value = [
    createMessage('assistant', '你好！我是AI助手，请问有什么可以帮助你的吗？')
  ]
}

// Markdown渲染
const renderMarkdown = (content: string) => {
  try {
    return marked(content, {
      breaks: true,
      gfm: true
    }) as string
  } catch (error) {
    console.error('Markdown parsing error:', error)
    return content
  }
}

// 处理文件选择变化
const handleFileSelectionChange = (value: number[]) => {
  selectedFiles.value = value
}

const selectAllFiles = () => {
  selectedFiles.value = knowledgeFiles.value.map(file => file.id)
}

const clearSelectedFiles = () => {
  selectedFiles.value = []
}

// 移除选中的文件
const removeSelectedFile = (fileId: number) => {
  const index = selectedFiles.value.indexOf(fileId)
  if (index > -1) {
    selectedFiles.value.splice(index, 1)
  }
}

// 根据文件ID获取文件名
const getFileNameById = (fileId: number) => {
  const file = knowledgeFiles.value.find(f => f.id === fileId)
  return file ? file.fileName : ''
}

// 根据文件ID获取截断的文件名
const getFileNameByLength = (fileId: number, maxLength: number) => {
  const fileName = getFileNameById(fileId)
  if (fileName.length <= maxLength) {
    return fileName
  }
  return fileName.substring(0, maxLength) + '...'
}

onMounted(() => {
  messages.value.push(createMessage('assistant', '你好！我是AI助手，请问有什么可以帮助你的吗？'))

  loadKnowledgeFiles()
})
</script>

<style scoped lang="less">
.rag-chat-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;

  h2 {
    margin: 0;
    font-size: 22px;
    color: var(--apple-text-primary);
  }

  p {
    margin: 6px 0 0;
    color: var(--apple-text-secondary);
  }
}

.header-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-card {
  min-width: 108px;
  padding: 12px 14px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-sm);

  strong {
    display: block;
    font-size: 20px;
    color: var(--apple-text-primary);
  }

  span {
    color: var(--apple-text-secondary);
    font-size: 12px;
  }
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 278px;
  gap: 16px;
  min-height: 0;
  flex: 1;
}

.chat-panel,
.inspector-panel {
  min-height: 0;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  box-shadow: var(--shadow-md);
}

.chat-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.source-panel {
  padding: 14px 16px;
  border-bottom: 1px solid var(--apple-border);
  background: rgba(255, 255, 255, 0.82);
}

.source-panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;

  strong {
    display: block;
    font-size: 14px;
    color: var(--apple-text-primary);
  }

  span {
    color: var(--apple-text-secondary);
    font-size: 12px;
  }
}

.source-actions {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
  align-items: center;
  padding: 4px;
  border: 1px solid var(--apple-border);
  border-radius: 999px;
  background: rgba(245, 245, 247, 0.9);
}

.source-actions :deep(.source-action-btn) {
  height: 30px;
  min-width: 68px;
  padding: 0 12px !important;
  margin-left: 0 !important;
  border: none !important;
  border-radius: 999px !important;
  box-shadow: none !important;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;

  .el-icon {
    margin-right: 4px;
    font-size: 14px;
  }
}

.source-actions :deep(.select-all-btn) {
  color: var(--apple-blue) !important;
  background: rgba(0, 122, 255, 0.1) !important;
}

.source-actions :deep(.select-all-btn:not(.is-disabled):hover) {
  color: #fff !important;
  background: var(--apple-blue) !important;
}

.source-actions :deep(.clear-btn) {
  color: var(--apple-text-secondary) !important;
  background: transparent !important;
}

.source-actions :deep(.clear-btn:not(.is-disabled):hover) {
  color: var(--apple-red) !important;
  background: rgba(255, 59, 48, 0.1) !important;
}

.source-actions :deep(.source-action-btn.is-disabled) {
  color: rgba(142, 142, 147, 0.55) !important;
  background: transparent !important;
  opacity: 1;
}

.source-select {
  width: 100%;

  :deep(.el-select__wrapper) {
    min-height: 40px;
    border-radius: 12px;
  }
}

.selected-files-popover {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
  max-height: 92px;
  overflow: auto;
}

.selected-files-empty {
  margin-top: 10px;
  color: var(--apple-text-secondary);
  font-size: 12px;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 18px 20px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.15);
    border-radius: 3px;
  }
}

.message {
  margin-bottom: 16px;
  max-width: 88%;
  animation: messageSlideIn 0.32s cubic-bezier(0.34, 1.56, 0.64, 1);

  &.user-message {
    margin-left: auto;

    .message-wrapper {
      flex-direction: row-reverse;
      justify-content: flex-start;
    }

    .message-content {
      background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%);
      color: #fff;
      border-bottom-right-radius: 6px;
    }

    .copy-button {
      margin-left: 8px;
    }
  }

  &.assistant-message {
    margin-right: auto;

    .message-content {
      background: rgba(255, 255, 255, 0.92);
      border: 1px solid var(--apple-border);
      color: var(--apple-text-primary);
      border-bottom-left-radius: 6px;
    }
  }
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.message-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-avatar {
  flex-shrink: 0;

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: #fff;
  }

  .user-avatar {
    background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%);
  }

  .assistant-avatar {
    background: linear-gradient(135deg, var(--apple-purple) 0%, var(--apple-indigo) 100%);
  }
}

.message-content {
  display: inline-block;
  max-width: 100%;
  padding: 12px 16px;
  border-radius: 18px;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.65;
  box-shadow: var(--shadow-sm);

  :deep(p) {
    margin: 0;
  }

  :deep(pre) {
    background-color: rgba(0, 0, 0, 0.05);
    padding: 12px;
    border-radius: var(--radius-sm);
    overflow-x: auto;
    font-size: 13px;
    margin: 8px 0;
  }

  :deep(code) {
    font-family: 'SF Mono', Consolas, Monaco, 'Andale Mono', monospace;
    background-color: rgba(0, 0, 0, 0.05);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 20px;
    margin: 8px 0;
  }

  :deep(blockquote) {
    margin: 8px 0;
    padding-left: 12px;
    border-left: 3px solid var(--apple-blue);
    color: var(--apple-text-secondary);
  }

  &.typing::after {
    content: '...';
    animation: ellipsis 1.5s infinite;
  }
}

@keyframes ellipsis {
  0% { content: '.'; }
  33% { content: '..'; }
  66% { content: '...'; }
  100% { content: '.'; }
}

.copy-button {
  opacity: 0;
  transition: opacity 0.2s ease;
  padding: 4px;
  height: auto;
  color: var(--apple-text-secondary);
  margin-right: 8px;

  &:hover {
    opacity: 1;
    color: var(--apple-blue);
  }
}

.message:hover .copy-button {
  opacity: 1;
}

.input-area {
  flex: 0 0 auto;
  border-top: 1px solid var(--apple-border);
  background: rgba(255, 255, 255, 0.82);
}

.input-container {
  display: flex;
  gap: 12px;
  padding: 16px 18px;
  align-items: flex-end;

  .el-textarea {
    flex: 1;

    :deep(.el-textarea__inner) {
      border-radius: var(--radius-md);
      padding: 12px 16px;
      font-size: 14px;
      line-height: 1.6;
    }
  }
}

.button-group {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.send-btn {
  background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%) !important;
  border: none !important;
  border-radius: var(--radius-sm) !important;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25) !important;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(0, 122, 255, 0.35) !important;
  }
}

.inspector-panel {
  padding: 14px;
}

.inspector-card {
  padding: 14px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.82);

  & + & {
    margin-top: 12px;
  }
}

.inspector-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-text-primary);
}

.inspector-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;

  strong {
    display: block;
    font-size: 20px;
    color: var(--apple-text-primary);
  }

  span {
    color: var(--apple-text-secondary);
    font-size: 12px;
  }
}

.inspector-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 360px;
  overflow: auto;
}

.inspector-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(245, 245, 247, 0.9);
  color: var(--apple-text-primary);
  font-size: 13px;
}

.inspector-note {
  margin: 0;
  color: var(--apple-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .inspector-panel {
    order: 2;
  }
}

@media (max-width: 760px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .input-container {
    flex-direction: column;
    align-items: stretch;
  }

  .button-group {
    justify-content: flex-end;
  }
}
</style>
