<template>
  <div class="page-container">
    <PageHeader title="对话式分析" subtitle="多轮对话，深入探索数据" />

    <a-row :gutter="16">
      <!-- Conversation History -->
      <a-col :xs="24" :md="6">
        <a-card title="对话历史" :bordered="false" class="page-card" size="small">
          <div class="conversation-list">
            <div
              v-for="conv in conversations"
              :key="conv.id"
              class="conversation-item"
              :class="{ active: currentConvId === conv.id }"
              @click="switchConversation(conv.id)"
            >
              <div class="conv-title">{{ conv.title }}</div>
              <div class="conv-meta">{{ conv.messageCount }} 条消息 · {{ conv.lastActive }}</div>
            </div>
          </div>
          <a-button type="dashed" block @click="startNewConversation" style="margin-top: 12px">
            <PlusOutlined /> 新对话
          </a-button>
        </a-card>
      </a-col>

      <!-- Chat Area -->
      <a-col :xs="24" :md="18">
        <a-card :bordered="false" class="page-card chat-card">
          <!-- Messages -->
          <div class="chat-messages" ref="messagesContainer">
            <div v-if="messages.length === 0" class="chat-empty">
              <RobotOutlined style="font-size: 48px; color: #d9d9d9" />
              <p>开始一个数据分析对话</p>
              <p class="chat-hint">试试问：最近3个月的销售趋势如何？</p>
            </div>

            <div v-for="(msg, idx) in messages" :key="idx" class="chat-message" :class="[`chat-message--${msg.role}`]">
              <div class="message-avatar">
                <a-avatar v-if="msg.role === 'user'" :style="{ background: '#1677ff' }">U</a-avatar>
                <a-avatar v-else :style="{ background: '#52c41a' }"><RobotOutlined /></a-avatar>
              </div>
              <div class="message-content">
                <div class="message-header">
                  <span class="message-role">{{ msg.role === 'user' ? '你' : 'AI 分析师' }}</span>
                  <span class="message-time">{{ msg.time }}</span>
                </div>
                <div class="message-body" v-html="renderMarkdown(msg.content)"></div>

                <!-- AI-specific: confidence, citations, chart -->
                <template v-if="msg.role === 'assistant'">
                  <div v-if="msg.confidence" class="message-confidence">
                    <span :style="{ color: getConfidenceColor(msg.confidence.score) }">
                      置信度: {{ msg.confidence.score }}% ({{ msg.confidence.level === 'high' ? '高' : msg.confidence.level === 'medium' ? '中' : '低' }})
                    </span>
                  </div>
                  <div v-if="msg.citations?.length" class="message-citations">
                    <span class="citations-label">数据来源：</span>
                    <a-tag v-for="(cite, ci) in msg.citations" :key="ci" size="small">{{ cite.sourceName }}</a-tag>
                  </div>
                  <div v-if="msg.chartData" class="message-chart">
                    <div :ref="(el: any) => { if (el) chartRefs[idx] = el }" style="height: 250px"></div>
                  </div>
                  <div v-if="msg.sql" class="message-sql">
                    <div class="sql-header">
                      <span>生成的 SQL</span>
                      <a-button type="link" size="small" @click="copySql(msg.sql!)">复制</a-button>
                    </div>
                    <pre class="sql-preview"><code>{{ msg.sql }}</code></pre>
                  </div>
                </template>
              </div>
            </div>

            <!-- Typing indicator -->
            <div v-if="loading" class="chat-message chat-message--assistant">
              <div class="message-avatar">
                <a-avatar :style="{ background: '#52c41a' }"><RobotOutlined /></a-avatar>
              </div>
              <div class="message-content">
                <div class="typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- Disclaimer -->
          <div class="chat-disclaimer" v-if="messages.some(m => m.role === 'assistant')">
            <ExclamationCircleOutlined /> AI 生成内容仅供参考，重要决策请核实原始数据
          </div>

          <!-- Input -->
          <div class="chat-input">
            <a-textarea
              v-model:value="inputText"
              :rows="2"
              placeholder="继续提问... 例如：按地区细分看看 / 和去年同期对比 / 预测下个月趋势"
              @pressEnter.ctrl="sendMessage"
              :disabled="loading"
            />
            <div class="chat-input-actions">
              <a-space>
                <a-select v-model:value="selectedDatasetId" placeholder="数据集" style="width: 160px" size="small">
                  <a-select-option v-for="ds in datasets" :key="ds.id" :value="ds.id">{{ ds.name }}</a-select-option>
                </a-select>
                <a-button type="primary" @click="sendMessage" :loading="loading" :disabled="!inputText.trim()">
                  <SendOutlined /> 发送
                </a-button>
                <a-dropdown>
                  <a-button size="small"><DownloadOutlined /></a-button>
                  <template #overlay>
                    <a-menu @click="handleExportChat">
                      <a-menu-item key="md">导出对话 Markdown</a-menu-item>
                      <a-menu-item key="json">导出对话 JSON</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  RobotOutlined,
  PlusOutlined,
  SendOutlined,
  DownloadOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { datasetApi, type Dataset } from '@/api/dataset'
import { post } from '@/api/request'
import { exportToJson, exportToMarkdown } from '@/utils/export'
import * as echarts from 'echarts'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  time: string
  sql?: string
  confidence?: { score: number; level: string; reasons: string[] }
  citations?: Array<{ sourceName: string; fields?: string[] }>
  chartData?: { type: string; categories: string[]; series: Array<{ name: string; data: number[] }> }
}

interface Conversation {
  id: string
  title: string
  messageCount: number
  lastActive: string
}

const inputText = ref('')
const loading = ref(false)
const messagesContainer = ref<HTMLElement>()
const selectedDatasetId = ref<string>('')
const currentConvId = ref('')
const datasets = ref<Dataset[]>([])
const chartRefs = reactive<Record<number, HTMLElement>>({})

const conversations = ref<Conversation[]>([])
const messages = ref<ChatMessage[]>([])

function getConfidenceColor(score: number): string {
  if (score >= 80) return '#52c41a'
  if (score >= 60) return '#faad14'
  return '#ff4d4f'
}

function renderMarkdown(text: string): string {
  // Simple markdown rendering
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}

function copySql(sql: string) {
  navigator.clipboard.writeText(sql)
  message.success('SQL已复制')
}

function startNewConversation() {
  const id = 'conv_' + Date.now()
  conversations.value.unshift({
    id,
    title: '新对话',
    messageCount: 0,
    lastActive: '刚刚',
  })
  currentConvId.value = id
  messages.value = []
}

function switchConversation(convId: string) {
  currentConvId.value = convId
  // In production, load messages from backend
  messages.value = []
}

async function sendMessage() {
  if (!inputText.value.trim() || loading.value) return

  const userMsg: ChatMessage = {
    role: 'user',
    content: inputText.value,
    time: new Date().toLocaleTimeString(),
  }
  messages.value.push(userMsg)
  const query = inputText.value
  inputText.value = ''

  // Update conversation
  const conv = conversations.value.find(c => c.id === currentConvId.value)
  if (conv) {
    conv.messageCount = messages.value.length
    conv.lastActive = '刚刚'
    if (conv.title === '新对话' && messages.value.length === 1) {
      conv.title = query.slice(0, 20) + (query.length > 20 ? '...' : '')
    }
  }

  await nextTick()
  scrollToBottom()

  loading.value = true
  try {
    // Build context from previous messages
    const context = messages.value
      .filter(m => m.role === 'user')
      .slice(-5)
      .map(m => m.content)

    const res = await post<{ data: any }>('/ai/analysis', {
      query,
      dataset_ids: selectedDatasetId.value ? [selectedDatasetId.value] : undefined,
      analysis_type: 'general',
      context, // Multi-turn context
    })

    const assistantMsg: ChatMessage = {
      role: 'assistant',
      content: res.data.analysis?.narrative || res.data.message || '分析完成',
      time: new Date().toLocaleTimeString(),
      sql: res.data.sql,
      confidence: res.data.confidence,
      citations: res.data.citations,
      chartData: res.data.chartData,
    }
    messages.value.push(assistantMsg)

    await nextTick()
    // Render charts
    if (assistantMsg.chartData) {
      const lastIdx = messages.value.length - 1
      const el = chartRefs[lastIdx]
      if (el) renderChart(el, assistantMsg.chartData)
    }
    scrollToBottom()
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，分析过程中出现错误，请稍后重试。',
      time: new Date().toLocaleTimeString(),
    })
  } finally {
    loading.value = false
  }
}

function renderChart(el: HTMLElement, chartData: any) {
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: chartData.categories },
    yAxis: { type: 'value' },
    series: chartData.series.map((s: any) => ({
      name: s.name,
      type: chartData.type || 'line',
      data: s.data,
      smooth: true,
    })),
  })
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function handleExportChat({ key }: { key: string }) {
  if (key === 'json') {
    exportToJson('chat_' + currentConvId.value, messages.value)
  } else {
    const cols = ['role', 'content', 'time']
    const rows = messages.value.map(m => ({ role: m.role, content: m.content, time: m.time }))
    exportToMarkdown('chat_' + currentConvId.value, cols, rows)
  }
}

onMounted(async () => {
  startNewConversation()
  try {
    const res = await datasetApi.list({ page: 1, size: 100 })
    datasets.value = res.data.items
  } catch {}
})
</script>

<style lang="scss" scoped>
.conversation-list {
  max-height: 400px;
  overflow-y: auto;
}

.conversation-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;

  &:hover { background: #f5f5f5; }
  &.active { background: #e6f4ff; border: 1px solid #91caff; }

  .conv-title {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .conv-meta {
    font-size: 11px;
    color: #999;
    margin-top: 2px;
  }
}

.chat-card {
  :deep(.ant-card-body) {
    padding: 0;
    display: flex;
    flex-direction: column;
    height: calc(100vh - 200px);
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.chat-empty {
  text-align: center;
  padding: 60px 0;
  color: #999;

  .chat-hint {
    font-size: 13px;
    color: #bbb;
    margin-top: 8px;
  }
}

.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

  &--user {
    flex-direction: row-reverse;

    .message-content {
      background: #e6f4ff;
      border-radius: 12px 0 12px 12px;
    }
  }

  &--assistant .message-content {
    background: #f6ffed;
    border-radius: 0 12px 12px 12px;
  }
}

.message-content {
  max-width: 80%;
  padding: 12px 16px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;

  .message-role {
    font-size: 12px;
    font-weight: 600;
    color: #666;
  }

  .message-time {
    font-size: 11px;
    color: #999;
  }
}

.message-body {
  font-size: 14px;
  line-height: 1.7;
  color: #333;

  :deep(code) {
    background: #f0f0f0;
    padding: 1px 4px;
    border-radius: 3px;
    font-size: 13px;
  }
}

.message-confidence {
  margin-top: 8px;
  font-size: 12px;
}

.message-citations {
  margin-top: 6px;
  font-size: 12px;
  color: #666;

  .citations-label { color: #999; }
}

.message-chart {
  margin-top: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.message-sql {
  margin-top: 10px;

  .sql-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: #666;
    margin-bottom: 4px;
  }

  .sql-preview {
    background: #1e1e1e;
    color: #d4d4d4;
    padding: 10px;
    border-radius: 6px;
    font-size: 12px;
    line-height: 1.5;
    max-height: 150px;
    overflow: auto;
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #999;
    animation: typing 1.4s infinite;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8px); opacity: 1; }
}

.chat-disclaimer {
  padding: 8px 20px;
  font-size: 12px;
  color: #faad14;
  background: #fffbe6;
  border-top: 1px solid #ffe58f;
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fff;

  .chat-input-actions {
    margin-top: 8px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
