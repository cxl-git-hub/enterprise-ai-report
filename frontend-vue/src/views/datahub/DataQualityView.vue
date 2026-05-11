<template>
  <div class="page-container">
    <PageHeader title="数据质量监控" subtitle="数据源健康检查与质量评分">
      <template #actions>
        <a-button type="primary" @click="runAllChecks" :loading="checking">
          <ThunderboltOutlined /> 执行全部检查
        </a-button>
      </template>
    </PageHeader>

    <!-- Overview Cards -->
    <a-row :gutter="16" class="stat-row">
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card" style="--accent: #52c41a; --accent-bg: #f6ffed">
          <div class="stat-value">{{ qualityScore }}</div>
          <div class="stat-label">综合质量评分</div>
          <a-progress :percent="qualityScore" :show-info="false" :stroke-color="qualityScoreColor" size="small" />
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card" style="--accent: #1677ff; --accent-bg: #e6f4ff">
          <div class="stat-value">{{ dataSourceCount }}</div>
          <div class="stat-label">数据源总数</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card" style="--accent: #faad14; --accent-bg: #fffbe6">
          <div class="stat-value">{{ alertCount }}</div>
          <div class="stat-label">质量告警</div>
        </div>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <div class="stat-card" style="--accent: #722ed1; --accent-bg: #f9f0ff">
          <div class="stat-value">{{ lastCheckTime }}</div>
          <div class="stat-label">上次检查</div>
        </div>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <!-- Data Source Health -->
      <a-col :xs="24" :md="16">
        <a-card title="数据源健康状态" :bordered="false" class="page-card">
          <a-table :columns="healthColumns" :data-source="healthData" :pagination="false" size="small" row-key="id">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-badge
                  :status="record.status === 'healthy' ? 'success' : record.status === 'warning' ? 'warning' : 'error'"
                  :text="record.status === 'healthy' ? '健康' : record.status === 'warning' ? '警告' : '异常'"
                />
              </template>
              <template v-if="column.key === 'freshness'">
                <a-tag :color="record.freshness === 'fresh' ? 'green' : record.freshness === 'stale' ? 'orange' : 'red'">
                  {{ record.freshness === 'fresh' ? '新鲜' : record.freshness === 'stale' ? '陈旧' : '过期' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'quality'">
                <a-progress :percent="record.qualityScore" :size="14" :show-info="false" :stroke-color="getQualityColor(record.qualityScore)" />
                <span style="margin-left: 8px; font-size: 12px">{{ record.qualityScore }}%</span>
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" size="small" @click="runCheck(record.id)" :loading="checkingIds.includes(record.id)">
                  检查
                </a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <!-- Alerts -->
      <a-col :xs="24" :md="8">
        <a-card title="质量告警" :bordered="false" class="page-card">
          <a-list :data-source="alerts" :split="false" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #avatar>
                    <a-avatar :style="{ background: item.level === 'error' ? '#ff4d4f' : '#faad14' }">
                      <ExclamationOutlined />
                    </a-avatar>
                  </template>
                  <template #title>{{ item.title }}</template>
                  <template #description>
                    <div>{{ item.description }}</div>
                    <div style="font-size: 11px; color: #999; margin-top: 4px">{{ item.time }}</div>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-if="alerts.length === 0" description="暂无告警" :image-style="{ height: '40px' }" />
        </a-card>

        <!-- Quality Rules -->
        <a-card title="质量规则" :bordered="false" class="page-card" style="margin-top: 16px">
          <a-list :data-source="qualityRules" :split="false" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a-space>
                      <a-switch :checked="item.enabled" size="small" @change="(v: boolean) => toggleRule(item.id, v)" />
                      {{ item.name }}
                    </a-space>
                  </template>
                  <template #description>{{ item.description }}</template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ThunderboltOutlined, ExclamationOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { get, post } from '@/api/request'

interface HealthRecord {
  id: string
  name: string
  type: string
  status: 'healthy' | 'warning' | 'error'
  freshness: 'fresh' | 'stale' | 'expired'
  qualityScore: number
  lastCheck: string
  rowCount: number
  nullRate: number
}

interface Alert {
  id: string
  level: 'warning' | 'error'
  title: string
  description: string
  time: string
}

interface QualityRule {
  id: string
  name: string
  description: string
  enabled: boolean
}

const checking = ref(false)
const checkingIds = ref<string[]>([])
const lastCheckTime = ref('5分钟前')

const healthData = ref<HealthRecord[]>([])

const alerts = ref<Alert[]>([
  { id: '1', level: 'warning', title: '数据质量检查', description: '点击"执行全部检查"按钮开始检测数据源健康状态', time: '系统提示' },
])

const qualityRules = ref<QualityRule[]>([
  { id: '1', name: '数据新鲜度检查', description: '检查数据源最后更新时间是否在阈值内', enabled: true },
  { id: '2', name: '空值率监控', description: '监控关键字段的空值率是否超限', enabled: true },
  { id: '3', name: '数据量异常检测', description: '检测数据量的突变（同比/环比）', enabled: true },
  { id: '4', name: '唯一性约束检查', description: '检查主键/唯一键是否有重复', enabled: false },
  { id: '5', name: '范围值检查', description: '检查数值字段是否在合理范围内', enabled: true },
])

const dataSourceCount = computed(() => healthData.value.length)
const alertCount = computed(() => alerts.value.length)
const qualityScore = computed(() => {
  const scores = healthData.value.map(h => h.qualityScore)
  return Math.round(scores.reduce((a, b) => a + b, 0) / scores.length)
})
const qualityScoreColor = computed(() => getQualityColor(qualityScore.value))

function getQualityColor(score: number): string {
  if (score >= 90) return '#52c41a'
  if (score >= 70) return '#faad14'
  return '#ff4d4f'
}

const healthColumns = [
  { title: '数据源', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '新鲜度', dataIndex: 'freshness', key: 'freshness', width: 80 },
  { title: '质量分', dataIndex: 'quality', key: 'quality', width: 120 },
  { title: '数据量', dataIndex: 'rowCount', key: 'rowCount', width: 120,
    customRender: ({ text }: { text: number }) => text?.toLocaleString() },
  { title: '空值率', dataIndex: 'nullRate', key: 'nullRate', width: 80,
    customRender: ({ text }: { text: number }) => `${(text * 100).toFixed(1)}%` },
  { title: '操作', key: 'action', width: 80 },
]

async function runAllChecks() {
  checking.value = true
  try {
    const res = await get<{ data: HealthRecord[] }>('/data-quality/health')
    if (res?.data) healthData.value = res.data
    lastCheckTime.value = '刚刚'
    message.success('全部检查完成')
  } catch {
    message.error('检查失败')
  } finally {
    checking.value = false
  }
}

async function runCheck(id: string) {
  checkingIds.value.push(id)
  try {
    await post(`/data-quality/check/${id}`, {})
    const record = healthData.value.find(h => h.id === id)
    if (record) record.lastCheck = '刚刚'
    message.success(`数据源检查完成`)
  } catch {
    message.error('检查失败')
  } finally {
    checkingIds.value = checkingIds.value.filter(i => i !== id)
  }
}

function toggleRule(id: string, enabled: boolean) {
  const rule = qualityRules.value.find(r => r.id === id)
  if (rule) rule.enabled = enabled
}

onMounted(async () => {
  try {
    const res = await get<{ data: HealthRecord[] }>('/data-quality/health')
    if (res?.data) healthData.value = res.data
  } catch {}
})
</script>

<style lang="scss" scoped>
.stat-row { margin-bottom: 16px; }

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  min-height: 100px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #1a1a1a;
  }

  .stat-label {
    font-size: 13px;
    color: #999;
    margin: 4px 0 8px;
  }
}
</style>
