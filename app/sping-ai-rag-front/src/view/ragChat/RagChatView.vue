<template>
  <div class="chat-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">AI 智能问答</span>
        </div>
      </template>

      <div class="chat-messages" ref="messageContainer">
        <div v-for="(message, index) in messages" :key="index"
             :class="['message', message.role === 'user' ? 'user-message' : 'assistant-message']">
          <div class="message-wrapper">
            <div class="message-avatar">
              <div v-if="message.role === 'user'" class="avatar user-avatar">
                <el-icon><User /></el-icon>
              </div>
              <div v-else class="avatar assistant-avatar">
                <el-icon><MagicStick /></el-icon>
              </div>
            </div>
            <div class="message-content" :class="{ 'typing': message.isTyping }" v-html="message.htmlContent || renderMarkdown(message.content)">
            </div>

            <el-button
              class="copy-button"
              type="text"
              size="small"
              @click="copyMessage(message.content)"
            >
              <el-icon><Document /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="file-selection-inline">
          <span class="file-selection-label">选择知识库文件：</span>
          <el-select
            v-model="selectedFiles"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="选择文件"
            style="width: 200px; margin-right: 10px;"
            size="default"
            @change="handleFileSelectionChange"
          >
            <el-option
              v-for="file in knowledgeFiles"
              :key="file.id"
              :label="file.fileName"
              :value="file.id"
            />
          </el-select>
          <el-popover
            v-if="selectedFiles.length > 0"
            trigger="hover"
            placement="top"
            :width="300"
          >
            <template #reference>
              <el-tag
                type="info"
                size="small"
                style="cursor: pointer; border-radius: 20px;"
              >
                已选{{ selectedFiles.length }}个
              </el-tag>
            </template>
            <div class="selected-files-popover">
              <div v-for="fileId in selectedFiles" :key="fileId" style="margin-bottom: 5px;">
                <el-tag
                  :title="getFileNameById(fileId)"
                  size="small"
                  closable
                  @close="removeSelectedFile(fileId)"
                  style="margin-right: 5px; margin-bottom: 5px;"
                >
                  {{ getFileNameByLength(fileId, 15) }}
                </el-tag>
              </div>
            </div>
          </el-popover>
        </div>

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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { marked } from 'marked'
import { Document, User, MagicStick, Position } from '@element-plus/icons-vue'
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
.chat-container {
  height: 100%;
  min-height: 0;
  padding: 0;
  display: flex;
  overflow: hidden;

  .box-card {
    width: 100%;
    height: 100%;
    min-height: 0;
    display: flex;
    flex-direction: column;
    border-radius: var(--radius-lg);
    border: none;
    box-shadow: var(--shadow-md);
    background: var(--apple-card);
    backdrop-filter: blur(20px);

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid var(--apple-border);
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.03) 0%, rgba(175, 82, 222, 0.03) 100%);
    }

    :deep(.el-card__body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 0;
      overflow: hidden;
    }
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .title {
    font-size: 18px;
    font-weight: 600;
    background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-purple) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.file-selection-inline {
  display: flex;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--apple-border);
  background: rgba(0, 0, 0, 0.02);
}

.file-selection-inline .file-selection-label {
  margin-right: 10px;
  font-weight: 500;
  color: var(--apple-text-primary);
  font-size: 14px;
}

.selected-files-popover {
  max-height: 200px;
  overflow-y: auto;
}

.chat-messages {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.15);
    border-radius: 3px;
  }

  &::-webkit-scrollbar-track {
    background-color: transparent;
  }
}

.message {
  margin-bottom: 20px;
  max-width: 85%;
  animation: messageSlideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);

  &.user-message {
    margin-left: auto;
    text-align: right;

    .message-wrapper {
      flex-direction: row-reverse;
      justify-content: flex-start;
    }

    .message-content {
      background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%);
      color: white;
      border-bottom-right-radius: 6px;
    }

    .copy-button {
      margin-left: 8px;
    }
  }

  &.assistant-message {
    margin-right: auto;
    text-align: left;

    .message-content {
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(10px);
      border: 1px solid var(--apple-border);
      color: var(--apple-text-primary);
      border-bottom-left-radius: 6px;
    }
  }
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.95);
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
  }

  .user-avatar {
    background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-indigo) 100%);
    color: white;
  }

  .assistant-avatar {
    background: linear-gradient(135deg, var(--apple-purple) 0%, var(--apple-indigo) 100%);
    color: white;
  }
}

.message-content {
  display: inline-block;
  max-width: 100%;
  padding: 12px 16px;
  border-radius: 18px;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
  box-shadow: var(--shadow-sm);

  :deep(p) {
    margin: 0;
    line-height: 1.6;
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

  :deep(ul), :deep(ol) {
    padding-left: 20px;
    margin: 8px 0;
  }

  :deep(blockquote) {
    margin: 8px 0;
    padding-left: 12px;
    border-left: 3px solid var(--apple-blue);
    color: var(--apple-text-secondary);
  }

  &.typing {
    &::after {
      content: '...';
      animation: ellipsis 1.5s infinite;
    }
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
  transition: opacity 0.3s;
  padding: 4px;
  height: auto;
  color: var(--apple-text-secondary);
  margin-right: 8px;

  &:hover {
    opacity: 1;
    color: var(--apple-blue);
  }
}

.input-area {
  flex: 0 0 auto;
  position: relative;
  z-index: 2;
  border-top: 1px solid var(--apple-border);
  background: rgba(255, 255, 255, 0.5);
}

.input-container {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
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

  .el-button:not(.send-btn) {
    border-radius: var(--radius-sm) !important;
  }
}
</style>
