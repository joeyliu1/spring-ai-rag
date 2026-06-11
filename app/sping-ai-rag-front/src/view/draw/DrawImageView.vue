<template>
  <div class="draw-workbench">
    <div class="page-header">
      <div>
        <h2>AI 绘画工作台</h2>
        <p>先写画面，再选风格和参数。生成结果会保留在当前页面，方便对比、下载和复用提示词。</p>
      </div>
      <div class="header-stats">
        <div class="stat-card">
          <strong>{{ generatedImages.length }}</strong>
          <span>当前结果</span>
        </div>
        <div class="stat-card">
          <strong>{{ historyItems.length }}</strong>
          <span>历史记录</span>
        </div>
      </div>
    </div>

    <div class="workbench-grid">
      <section class="composer-panel panel">
        <div class="panel-head">
          <div>
            <strong>提示词</strong>
            <span>描述主体、场景、光线、构图和色调</span>
          </div>
          <el-button text @click="clearPrompt" :disabled="!prompt && !negativePrompt">
            <el-icon><RefreshLeft /></el-icon>
            清空
          </el-button>
        </div>

        <el-input
          v-model="prompt"
          type="textarea"
          :rows="7"
          resize="none"
          placeholder="例如：一座雨夜中的未来城市，霓虹灯反射在街道积水里，远处有悬浮列车，电影感构图，柔和体积光"
          class="prompt-input"
        />

        <div class="quick-section">
          <div class="section-title">灵感模板</div>
          <div class="chip-row">
            <button
              v-for="item in inspirationPrompts"
              :key="item.title"
              class="chip"
              @click="applyInspiration(item.prompt)"
            >
              {{ item.title }}
            </button>
          </div>
        </div>

        <div class="quick-section">
          <div class="section-title">风格</div>
          <div class="chip-row">
            <button
              v-for="item in styleOptions"
              :key="item.value"
              class="chip"
              :class="{ active: selectedStyle === item.value }"
              @click="selectedStyle = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </div>

        <div class="params-grid">
          <label class="param-card">
            <span>画面比例</span>
            <el-select v-model="ratio" size="large">
              <el-option v-for="item in ratioOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="param-card">
            <span>生成张数</span>
            <el-select v-model="imageCount" size="large">
              <el-option :label="'1 张'" :value="1" />
              <el-option :label="'2 张'" :value="2" />
              <el-option :label="'4 张'" :value="4" />
            </el-select>
          </label>
          <label class="param-card">
            <span>风格强度</span>
            <el-slider v-model="styleStrength" :min="0" :max="100" :step="10" />
          </label>
          <label class="param-card">
            <span>随机种子</span>
            <el-input v-model="seed" size="large" placeholder="自动" clearable />
          </label>
        </div>

        <el-collapse class="advanced-collapse">
          <el-collapse-item title="负面提示词" name="negative">
            <el-input
              v-model="negativePrompt"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="不希望出现的元素，例如：低清晰度、变形、文字、水印、过曝"
            />
          </el-collapse-item>
        </el-collapse>

        <div class="action-row">
          <div class="secondary-actions">
            <el-button @click="randomPrompt">
              <el-icon><MagicStick /></el-icon>
              随机灵感
            </el-button>
            <el-button @click="resetParams">重置参数</el-button>
          </div>
          <el-button
            type="primary"
            size="large"
            class="generate-btn"
            :loading="isLoading"
            :disabled="!prompt.trim()"
            @click="generateImage"
          >
            <el-icon v-if="!isLoading"><PictureRounded /></el-icon>
            {{ isLoading ? `生成中 ${generationProgress}/${imageCount}` : "生成图片" }}
          </el-button>
        </div>
      </section>

      <aside class="side-panel">
        <section class="panel tips-panel">
          <div class="panel-head compact">
            <div>
              <strong>生成设置</strong>
              <span>会自动拼接到提示词中</span>
            </div>
          </div>
          <div class="setting-list">
            <div>
              <span>风格</span>
              <strong>{{ currentStyleLabel }}</strong>
            </div>
            <div>
              <span>比例</span>
              <strong>{{ currentRatioLabel }}</strong>
            </div>
            <div>
              <span>强度</span>
              <strong>{{ styleStrength }}%</strong>
            </div>
            <div>
              <span>种子</span>
              <strong>{{ seed || "自动" }}</strong>
            </div>
          </div>
        </section>

        <section class="panel history-panel">
          <div class="panel-head compact">
            <div>
              <strong>最近生成</strong>
              <span>本次会话内保留</span>
            </div>
            <el-button text size="small" :disabled="historyItems.length === 0" @click="clearHistory">清空</el-button>
          </div>
          <div v-if="historyItems.length" class="history-list">
            <button
              v-for="item in historyItems"
              :key="item.id"
              class="history-item"
              @click="reusePrompt(item)"
            >
              <strong>{{ item.prompt }}</strong>
              <span>{{ item.createdAt }} · {{ item.styleLabel }}</span>
            </button>
          </div>
          <div v-else class="empty-history">
            生成后会在这里显示最近记录。
          </div>
        </section>
      </aside>
    </div>

    <section class="result-panel panel">
      <div class="panel-head">
        <div>
          <strong>生成结果</strong>
          <span>可预览、下载、复制提示词或继续复用</span>
        </div>
        <el-button :disabled="generatedImages.length === 0" @click="downloadAll">
          <el-icon><Download /></el-icon>
          全部下载
        </el-button>
      </div>

      <div v-if="isLoading && generatedImages.length === 0" class="loading-state">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <strong>正在生成图片</strong>
        <span>请稍等，结果会自动显示在下方。</span>
      </div>

      <div v-else-if="generatedImages.length === 0" class="empty-result">
        <div class="empty-visual">
          <el-icon><PictureRounded /></el-icon>
        </div>
        <strong>还没有生成结果</strong>
        <span>输入提示词并点击生成图片后，结果会显示在这里。</span>
      </div>

      <div v-else class="result-grid">
        <article v-for="image in generatedImages" :key="image.id" class="image-card">
          <button class="image-preview" @click="openPreview(image)">
            <img :src="image.url" alt="生成的图片" />
          </button>
          <div class="image-meta">
            <div>
              <strong>{{ image.title }}</strong>
              <span>{{ image.ratioLabel }} · {{ image.styleLabel }}</span>
            </div>
            <div class="image-actions">
              <el-button circle size="small" @click="downloadImage(image)">
                <el-icon><Download /></el-icon>
              </el-button>
              <el-button circle size="small" @click="copyPrompt(image.prompt)">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
              <el-button circle size="small" @click="reusePrompt(image)">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </div>
        </article>
      </div>
    </section>

    <el-dialog
      v-model="previewVisible"
      title="图片预览"
      width="min(900px, calc(100vw - 32px))"
      align-center
      append-to-body
      class="image-dialog"
    >
      <img v-if="previewImage" :src="previewImage.url" class="dialog-image" alt="生成的图片预览" />
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button v-if="previewImage" type="primary" @click="downloadImage(previewImage)">下载图片</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DocumentCopy,
  Download,
  Loading,
  MagicStick,
  PictureRounded,
  Refresh,
  RefreshLeft,
} from '@element-plus/icons-vue'
import { drawApi } from '@/api/DrawApi'

interface GeneratedImage {
  id: string
  url: string
  prompt: string
  title: string
  style: string
  styleLabel: string
  ratio: string
  ratioLabel: string
  createdAt: string
}

const prompt = ref('')
const negativePrompt = ref('')
const selectedStyle = ref('realistic')
const ratio = ref('1:1')
const imageCount = ref(1)
const styleStrength = ref(60)
const seed = ref('')
const generatedImages = ref<GeneratedImage[]>([])
const historyItems = ref<GeneratedImage[]>([])
const isLoading = ref(false)
const generationProgress = ref(0)
const previewVisible = ref(false)
const previewImage = ref<GeneratedImage | null>(null)

const styleOptions = [
  { label: '写实', value: 'realistic', prompt: '写实摄影风格，真实光影，高细节' },
  { label: '国风', value: 'chinese', prompt: '国风美学，水墨意境，东方色彩' },
  { label: '赛博朋克', value: 'cyberpunk', prompt: '赛博朋克风格，霓虹灯，未来城市' },
  { label: '水彩', value: 'watercolor', prompt: '水彩插画风格，柔和笔触，清透色彩' },
  { label: '插画', value: 'illustration', prompt: '精致商业插画，清晰轮廓，丰富细节' },
  { label: '极简', value: 'minimal', prompt: '极简设计风格，干净构图，低饱和色彩' },
]

const ratioOptions = [
  { label: '正方形 1:1', value: '1:1' },
  { label: '横版 16:9', value: '16:9' },
  { label: '竖版 9:16', value: '9:16' },
  { label: '海报 3:4', value: '3:4' },
]

const inspirationPrompts = [
  { title: '未来城市', prompt: '雨夜中的未来城市，霓虹灯反射在街道积水里，远处有悬浮列车，电影感构图' },
  { title: '国风山水', prompt: '云雾缭绕的山水庭院，晨光穿过竹林，远处亭台楼阁，东方留白构图' },
  { title: '产品海报', prompt: '一瓶高级香水悬浮在柔和光线中，玻璃质感，干净背景，商业广告摄影' },
  { title: '人物肖像', prompt: '年轻创作者坐在工作室窗边，柔和自然光，浅景深，写实摄影风格' },
]

const currentStyle = computed(() => styleOptions.find((item) => item.value === selectedStyle.value) || styleOptions[0])
const currentStyleLabel = computed(() => currentStyle.value.label)
const currentRatioLabel = computed(() => ratioOptions.find((item) => item.value === ratio.value)?.label || ratio.value)

const buildPrompt = () => {
  const parts = [
    prompt.value.trim(),
    currentStyle.value.prompt,
    `画面比例 ${ratio.value}`,
    `风格强度 ${styleStrength.value}%`,
  ]
  if (seed.value.trim()) {
    parts.push(`随机种子 ${seed.value.trim()}`)
  }
  if (negativePrompt.value.trim()) {
    parts.push(`避免出现：${negativePrompt.value.trim()}`)
  }
  return parts.filter(Boolean).join('，')
}

const formatTime = () => {
  return new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

const generateImage = async () => {
  if (!prompt.value.trim() || isLoading.value) return

  isLoading.value = true
  generationProgress.value = 0
  revokeImages(generatedImages.value)
  generatedImages.value = []
  const finalPrompt = buildPrompt()

  try {
    for (let index = 0; index < imageCount.value; index += 1) {
      const blob = await drawApi(finalPrompt)
      const url = URL.createObjectURL(blob)
      const image: GeneratedImage = {
        id: `${Date.now()}-${index}`,
        url,
        prompt: prompt.value.trim(),
        title: `结果 ${String(index + 1).padStart(2, '0')}`,
        style: selectedStyle.value,
        styleLabel: currentStyleLabel.value,
        ratio: ratio.value,
        ratioLabel: currentRatioLabel.value,
        createdAt: formatTime(),
      }
      generatedImages.value.push(image)
      generationProgress.value = index + 1
    }
    prependHistory(generatedImages.value[0])
    ElMessage.success('图片生成成功')
  } catch (error) {
    console.error('生成图片失败:', error)
    ElMessage.error('生成图片失败，请稍后再试')
  } finally {
    isLoading.value = false
  }
}

const prependHistory = (image?: GeneratedImage) => {
  if (!image) return
  historyItems.value = [image, ...historyItems.value].slice(0, 8)
}

const applyInspiration = (value: string) => {
  prompt.value = value
}

const randomPrompt = () => {
  const next = inspirationPrompts[Math.floor(Math.random() * inspirationPrompts.length)]
  prompt.value = next.prompt
}

const clearPrompt = () => {
  prompt.value = ''
  negativePrompt.value = ''
}

const resetParams = () => {
  selectedStyle.value = 'realistic'
  ratio.value = '1:1'
  imageCount.value = 1
  styleStrength.value = 60
  seed.value = ''
}

const downloadImage = (image: GeneratedImage) => {
  const link = document.createElement('a')
  link.href = image.url
  link.download = `generated-image-${Date.now()}.png`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const downloadAll = () => {
  generatedImages.value.forEach(downloadImage)
}

const copyPrompt = async (value: string) => {
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success('提示词已复制')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

const reusePrompt = (image: GeneratedImage) => {
  prompt.value = image.prompt
  selectedStyle.value = image.style
  ratio.value = image.ratio
}

const openPreview = (image: GeneratedImage) => {
  previewImage.value = image
  previewVisible.value = true
}

const clearHistory = () => {
  revokeImages(historyItems.value)
  historyItems.value = []
}

const revokeImages = (items: GeneratedImage[]) => {
  items.forEach((item) => {
    const isStillVisible = generatedImages.value.some((image) => image.url === item.url)
    const isInHistory = historyItems.value.some((image) => image.url === item.url)
    if (!isStillVisible && !isInHistory) {
      URL.revokeObjectURL(item.url)
    }
  })
}

onBeforeUnmount(() => {
  const imageUrls = new Set<string>()
  generatedImages.value.forEach((item) => imageUrls.add(item.url))
  historyItems.value.forEach((item) => imageUrls.add(item.url))
  imageUrls.forEach((url) => URL.revokeObjectURL(url))
})
</script>

<style scoped lang="less">
.draw-workbench {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;

  h2 {
    margin: 0;
    color: var(--apple-text-primary);
    font-size: 22px;
  }

  p {
    margin: 6px 0 0;
    max-width: 760px;
    color: var(--apple-text-secondary);
    line-height: 1.6;
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
    color: var(--apple-text-primary);
    font-size: 20px;
  }

  span {
    color: var(--apple-text-secondary);
    font-size: 12px;
  }
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.panel {
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px);
  box-shadow: var(--shadow-sm);
  min-width: 0;
}

.composer-panel {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;

  strong {
    display: block;
    color: var(--apple-text-primary);
    font-size: 16px;
  }

  span {
    display: block;
    margin-top: 4px;
    color: var(--apple-text-secondary);
    font-size: 13px;
  }
}

.panel-head.compact {
  padding: 16px 16px 0;
}

.prompt-input {
  :deep(.el-textarea__inner) {
    border-radius: var(--radius-lg);
    border: 1px solid var(--apple-border);
    box-shadow: none;
    color: var(--apple-text-primary);
    font-size: 15px;
    line-height: 1.65;
  }
}

.quick-section {
  display: grid;
  gap: 8px;
}

.section-title {
  color: var(--apple-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--apple-border);
  border-radius: 999px;
  color: var(--apple-text-primary);
  background: rgba(255, 255, 255, 0.82);
  cursor: pointer;
  transition: all 0.2s var(--ease-apple);

  &:hover {
    border-color: var(--apple-blue);
    color: var(--apple-blue);
  }

  &.active {
    color: #fff;
    border-color: var(--apple-blue);
    background: linear-gradient(135deg, var(--apple-blue), var(--apple-indigo));
    box-shadow: var(--shadow-button);
  }
}

.params-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.param-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.64);

  span {
    display: block;
    margin-bottom: 8px;
    color: var(--apple-text-secondary);
    font-size: 12px;
    font-weight: 600;
  }

  :deep(.el-select),
  :deep(.el-input) {
    width: 100%;
  }
}

.advanced-collapse {
  border-top: 1px solid var(--apple-border);
  border-bottom: 1px solid var(--apple-border);

  :deep(.el-collapse-item__header),
  :deep(.el-collapse-item__wrap) {
    background: transparent;
  }
}

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.secondary-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.generate-btn {
  min-width: 156px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.setting-list {
  padding: 16px;
  display: grid;
  gap: 10px;

  div {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 12px;
    border-radius: var(--radius-md);
    background: rgba(255, 255, 255, 0.7);
  }

  span {
    color: var(--apple-text-secondary);
  }

  strong {
    color: var(--apple-text-primary);
  }
}

.history-panel {
  flex: 1;
  min-height: 280px;
}

.history-list {
  padding: 16px;
  display: grid;
  gap: 10px;
  max-height: 420px;
  overflow-y: auto;
}

.history-item {
  width: 100%;
  text-align: left;
  padding: 12px;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;

  strong {
    display: -webkit-box;
    overflow: hidden;
    color: var(--apple-text-primary);
    font-size: 13px;
    line-height: 1.45;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  span {
    display: block;
    margin-top: 6px;
    color: var(--apple-text-secondary);
    font-size: 12px;
  }
}

.empty-history,
.empty-result,
.loading-state {
  color: var(--apple-text-secondary);
}

.empty-history {
  padding: 16px;
  line-height: 1.6;
}

.result-panel {
  overflow: hidden;

  > .panel-head {
    padding: 16px;
    border-bottom: 1px solid var(--apple-border);
  }
}

.result-grid {
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.image-card {
  overflow: hidden;
  border: 1px solid var(--apple-border);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: var(--shadow-sm);
}

.image-preview {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: #f5f5f7;
  cursor: zoom-in;

  img {
    display: block;
    width: 100%;
    aspect-ratio: 1 / 1;
    object-fit: contain;
  }
}

.image-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px;

  strong {
    display: block;
    color: var(--apple-text-primary);
    font-size: 14px;
  }

  span {
    display: block;
    margin-top: 4px;
    color: var(--apple-text-secondary);
    font-size: 12px;
  }
}

.image-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.empty-result,
.loading-state {
  min-height: 300px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  padding: 32px;

  strong {
    color: var(--apple-text-primary);
    font-size: 16px;
  }
}

.empty-visual,
.loading-icon {
  width: 62px;
  height: 62px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  color: var(--apple-blue);
  background: rgba(0, 122, 255, 0.08);
  font-size: 28px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

.dialog-image {
  display: block;
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: var(--radius-md);
  background: #f5f5f7;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1180px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .params-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .side-panel,
  .params-grid {
    grid-template-columns: 1fr;
  }

  .action-row {
    align-items: stretch;
  }

  .generate-btn {
    width: 100%;
  }
}
</style>
