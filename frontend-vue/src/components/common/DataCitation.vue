<template>
  <div class="data-citation">
    <div v-if="citations.length" class="citation-list">
      <div class="citation-header">
        <LinkOutlined /> 数据溯源
      </div>
      <div v-for="(cite, idx) in citations" :key="idx" class="citation-item" @click="$emit('navigate', cite)">
        <div class="citation-badge">{{ idx + 1 }}</div>
        <div class="citation-body">
          <div class="citation-source">
            <DatabaseOutlined /> {{ cite.sourceName }}
            <a-tag v-if="cite.timeRange" size="small" color="blue">{{ cite.timeRange }}</a-tag>
          </div>
          <div v-if="cite.fields?.length" class="citation-fields">
            <span class="fields-label">涉及字段：</span>
            <a-tag v-for="f in cite.fields" :key="f" size="small">{{ f }}</a-tag>
          </div>
          <div v-if="cite.description" class="citation-desc">{{ cite.description }}</div>
          <div class="citation-meta">
            <span v-if="cite.rowCount">数据量：{{ formatNumber(cite.rowCount) }} 行</span>
            <span v-if="cite.lastUpdated">最后更新：{{ cite.lastUpdated }}</span>
            <span v-if="cite.quality" :class="['quality-badge', `quality-${cite.quality}`]">
              数据质量：{{ qualityLabel[cite.quality] }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { DatabaseOutlined, LinkOutlined } from '@ant-design/icons-vue'

export interface Citation {
  sourceName: string
  datasetId?: string
  schemaId?: string
  fields?: string[]
  timeRange?: string
  description?: string
  rowCount?: number
  lastUpdated?: string
  quality?: 'high' | 'medium' | 'low'
  sql?: string
}

defineProps<{
  citations: Citation[]
}>()

defineEmits<{
  navigate: [citation: Citation]
}>()

const qualityLabel: Record<string, string> = {
  high: '高',
  medium: '中',
  low: '低',
}

function formatNumber(n: number): string {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`
  return n.toString()
}
</script>

<style lang="scss" scoped>
.data-citation {
  margin: 12px 0;
}

.citation-header {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.citation-item {
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #91caff;
    background: #f0f7ff;
  }
}

.citation-badge {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #1677ff;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.citation-body {
  flex: 1;
  min-width: 0;
}

.citation-source {
  font-weight: 600;
  font-size: 13px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 6px;
}

.citation-fields {
  margin-top: 4px;
  font-size: 12px;
  color: #666;

  .fields-label { color: #999; }
}

.citation-desc {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}

.citation-meta {
  margin-top: 6px;
  font-size: 11px;
  color: #999;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  .quality-badge {
    &.quality-high { color: #52c41a; }
    &.quality-medium { color: #faad14; }
    &.quality-low { color: #ff4d4f; }
  }
}
</style>
