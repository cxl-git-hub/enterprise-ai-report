<template>
  <div class="ai-disclaimer" :class="[`ai-disclaimer--${level}`, { 'ai-disclaimer--compact': compact }]">
    <div class="ai-disclaimer__icon">
      <ExclamationCircleOutlined v-if="level === 'warning'" />
      <InfoCircleOutlined v-else />
    </div>
    <div class="ai-disclaimer__content">
      <div class="ai-disclaimer__label">
        <a-tag :color="tagColor" size="small">{{ tagText }}</a-tag>
        <span v-if="confidence !== null" class="ai-disclaimer__confidence">
          置信度：<a-progress :percent="confidence" :size="14" :show-info="false" :stroke-color="confidenceColor" />
          <span :style="{ color: confidenceColor }">{{ confidenceLabel }}</span>
        </span>
      </div>
      <div class="ai-disclaimer__text">
        {{ text || defaultText }}
      </div>
      <div v-if="sources?.length" class="ai-disclaimer__sources">
        <span class="sources-label">数据来源：</span>
        <a-tag v-for="(src, idx) in sources" :key="idx" size="small" class="source-tag" @click="$emit('source-click', src)">
          <DatabaseOutlined /> {{ src.label }}
        </a-tag>
      </div>
      <div v-if="showDetails" class="ai-disclaimer__details">
        <slot name="details" />
      </div>
    </div>
    <div v-if="dismissible" class="ai-disclaimer__action">
      <a-button type="text" size="small" @click="$emit('dismiss')">
        <CloseOutlined />
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ExclamationCircleOutlined, InfoCircleOutlined, DatabaseOutlined, CloseOutlined } from '@ant-design/icons-vue'

interface DataSource {
  label: string
  datasetId?: string
  schemaId?: string
  fields?: string[]
  timeRange?: string
}

const props = withDefaults(defineProps<{
  level?: 'info' | 'warning'
  type?: 'report' | 'analysis' | 'nl2sql' | 'suggestion'
  text?: string
  confidence?: number | null
  sources?: DataSource[]
  compact?: boolean
  dismissible?: boolean
  showDetails?: boolean
}>(), {
  level: 'warning',
  type: 'report',
  confidence: null,
  compact: false,
  dismissible: false,
  showDetails: false,
})

defineEmits<{
  dismiss: []
  'source-click': [source: DataSource]
}>()

const defaultText = computed(() => {
  const texts: Record<string, string> = {
    report: '本报告由 AI 辅助生成，数据仅供参考，请以原始数据源为准。基于 AI 生成的内容可能存在偏差，重要决策请核实原始数据。',
    analysis: '以下分析结论由 AI 基于数据自动生成，仅供参考。AI 可能无法完全理解业务上下文，请结合实际业务判断。',
    nl2sql: '以下 SQL 由 AI 根据自然语言自动生成，执行前请确认 SQL 逻辑正确性。',
    suggestion: '以下建议由 AI 生成，仅供参考。请根据实际业务需求进行调整。',
  }
  return texts[props.type] || texts.report
})

const tagColor = computed(() => {
  if (props.type === 'nl2sql') return 'blue'
  if (props.type === 'analysis') return 'purple'
  if (props.type === 'suggestion') return 'cyan'
  return props.level === 'warning' ? 'orange' : 'blue'
})

const tagText = computed(() => {
  const labels: Record<string, string> = {
    report: 'AI 生成报告',
    analysis: 'AI 分析结论',
    nl2sql: 'AI 生成 SQL',
    suggestion: 'AI 建议',
  }
  return labels[props.type] || 'AI 生成内容'
})

const confidenceColor = computed(() => {
  if (props.confidence === null) return '#999'
  if (props.confidence >= 80) return '#52c41a'
  if (props.confidence >= 60) return '#faad14'
  return '#ff4d4f'
})

const confidenceLabel = computed(() => {
  if (props.confidence === null) return ''
  if (props.confidence >= 80) return '高'
  if (props.confidence >= 60) return '中'
  return '低'
})
</script>

<style lang="scss" scoped>
.ai-disclaimer {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid;
  margin-bottom: 16px;

  &--warning {
    background: #fffbe6;
    border-color: #ffe58f;
  }

  &--info {
    background: #e6f4ff;
    border-color: #91caff;
  }

  &--compact {
    padding: 8px 12px;
    margin-bottom: 8px;

    .ai-disclaimer__text { font-size: 12px; }
  }

  &__icon {
    font-size: 18px;
    margin-top: 2px;
    flex-shrink: 0;

    .ai-disclaimer--warning & { color: #faad14; }
    .ai-disclaimer--info & { color: #1677ff; }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__label {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 6px;
    flex-wrap: wrap;
  }

  &__confidence {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #666;

    :deep(.ant-progress) {
      width: 60px;
    }
  }

  &__text {
    font-size: 13px;
    color: #666;
    line-height: 1.6;
  }

  &__sources {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;

    .sources-label {
      font-size: 12px;
      color: #999;
    }

    .source-tag {
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: #1677ff;
        color: #1677ff;
      }
    }
  }

  &__details {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px dashed #e8e8e8;
  }

  &__action {
    flex-shrink: 0;
  }
}
</style>
