<template>
  <div class="page-container">
    <PageHeader title="模板市场" subtitle="行业预置报表模板，一键使用" />

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" class="search-bar">
        <a-form-item>
          <a-input v-model:value="searchKeyword" placeholder="搜索模板" allow-clear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="selectedCategory" placeholder="分类" allow-clear style="width: 140px">
            <a-select-option v-for="cat in categories" :key="cat.value" :value="cat.value">{{ cat.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="selectedFormat" placeholder="格式" allow-clear style="width: 100px">
            <a-select-option value="pdf">PDF</a-select-option>
            <a-select-option value="docx">Word</a-select-option>
            <a-select-option value="excel">Excel</a-select-option>
            <a-select-option value="pptx">PPT</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <a-row :gutter="[16, 16]" style="margin-top: 16px">
        <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="template in filteredTemplates" :key="template.id">
          <a-card hoverable class="template-card" @click="handlePreview(template)">
            <template #cover>
              <div class="template-cover" :style="{ background: template.coverColor }">
                <div class="template-icon">
                  <FileTextOutlined />
                </div>
                <div class="template-format">
                  <a-tag :color="formatTagColor[template.format]">{{ template.format.toUpperCase() }}</a-tag>
                </div>
              </div>
            </template>
            <a-card-meta :title="template.name">
              <template #description>
                <div class="template-desc">{{ template.description }}</div>
                <div class="template-meta">
                  <a-tag size="small">{{ template.category }}</a-tag>
                  <span class="template-usage">
                    <DownloadOutlined /> {{ template.usageCount }} 次使用
                  </span>
                </div>
              </template>
            </a-card-meta>
          </a-card>
        </a-col>
      </a-row>

      <a-empty v-if="filteredTemplates.length === 0" description="没有找到匹配的模板" />
    </a-card>

    <!-- Preview Modal -->
    <a-modal
      v-model:open="previewVisible"
      :title="previewTemplate?.name"
      width="800px"
      :footer="null"
    >
      <div v-if="previewTemplate">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="分类">{{ previewTemplate.category }}</a-descriptions-item>
          <a-descriptions-item label="格式">{{ previewTemplate.format.toUpperCase() }}</a-descriptions-item>
          <a-descriptions-item label="使用次数">{{ previewTemplate.usageCount }}</a-descriptions-item>
          <a-descriptions-item label="版本">{{ previewTemplate.version }}</a-descriptions-item>
        </a-descriptions>
        <div class="preview-content">
          <h4>模板预览</h4>
          <div class="preview-body" v-html="previewTemplate.previewHtml"></div>
        </div>
        <div class="preview-actions" style="margin-top: 16px; text-align: right">
          <a-button @click="handleCopyTemplate">
            <CopyOutlined /> 复制模板
          </a-button>
          <a-button type="primary" @click="handleUseTemplate" style="margin-left: 8px">
            <ThunderboltOutlined /> 使用此模板
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, FileTextOutlined, DownloadOutlined, CopyOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { post } from '@/api/request'

interface MarketTemplate {
  id: string
  name: string
  description: string
  category: string
  format: string
  coverColor: string
  usageCount: number
  version: string
  templateContent: string
  previewHtml: string
}

const searchKeyword = ref('')
const selectedCategory = ref('')
const selectedFormat = ref('')
const previewVisible = ref(false)
const previewTemplate = ref<MarketTemplate | null>(null)

const categories = [
  { value: 'finance', label: '财务报表' },
  { value: 'sales', label: '销售分析' },
  { value: 'operations', label: '运营报告' },
  { value: 'hr', label: '人力资源' },
  { value: 'marketing', label: '市场营销' },
  { value: 'supply', label: '供应链' },
]

const formatTagColor: Record<string, string> = {
  pdf: 'red',
  docx: 'purple',
  excel: 'green',
  pptx: 'orange',
  html: 'blue',
}

// Preset templates
const presetTemplates: MarketTemplate[] = [
  {
    id: 'tpl_monthly_finance',
    name: '月度财务分析报告',
    description: '包含收入、支出、利润、现金流等核心财务指标的月度分析报告',
    category: 'finance',
    format: 'pdf',
    coverColor: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    usageCount: 1280,
    version: 'v2.1',
    templateContent: '# {{title}}\n\n## 财务概览\n{{overview}}\n\n## 收入分析\n{{revenue}}\n\n## 支出分析\n{{expense}}\n\n## 利润趋势\n{{profit_trend}}\n\n## 现金流\n{{cashflow}}\n\n## 建议\n{{recommendations}}',
    previewHtml: '<h1>月度财务分析报告</h1><h2>财务概览</h2><p>本月总收入: ¥1,234,567 | 同比增长: +15.3%</p><h2>收入分析</h2><p>按产品线分解...</p><h2>支出分析</h2><p>主要支出项...</p>',
  },
  {
    id: 'tpl_weekly_sales',
    name: '周度销售业绩报表',
    description: '销售团队周度业绩汇总，包含销售额、订单量、客户转化率等',
    category: 'sales',
    format: 'excel',
    coverColor: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    usageCount: 956,
    version: 'v1.8',
    templateContent: '# 周度销售业绩\n\n## 业绩总览\n{{summary}}\n\n## 区域分布\n{{by_region}}\n\n## 产品排名\n{{product_ranking}}\n\n## 客户转化\n{{conversion}}',
    previewHtml: '<h1>周度销售业绩报表</h1><p>本周总销售额: ¥567,890</p><p>订单量: 234 | 转化率: 12.3%</p>',
  },
  {
    id: 'tpl_daily_ops',
    name: '每日运营监控报告',
    description: '系统运行状态、关键业务指标的每日监控报告',
    category: 'operations',
    format: 'pdf',
    coverColor: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    usageCount: 2100,
    version: 'v3.0',
    templateContent: '# 每日运营监控 {{date}}\n\n## 系统状态\n{{system_status}}\n\n## 核心指标\n{{kpis}}\n\n## 异常告警\n{{alerts}}\n\n## 趋势\n{{trends}}',
    previewHtml: '<h1>每日运营监控报告</h1><p>系统可用性: 99.97%</p><p>今日处理请求: 1,234,567</p><p>异常告警: 2</p>',
  },
  {
    id: 'tpl_quarterly_hr',
    name: '季度人力资源报告',
    description: '员工人数、流动率、培训完成率等HR核心指标季度报告',
    category: 'hr',
    format: 'docx',
    coverColor: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    usageCount: 432,
    version: 'v1.5',
    templateContent: '# 季度HR报告 Q{{quarter}}\n\n## 人员概况\n{{headcount}}\n\n## 招聘情况\n{{recruitment}}\n\n## 员工流动\n{{turnover}}\n\n## 培训发展\n{{training}}',
    previewHtml: '<h1>季度人力资源报告 Q1</h1><p>在职员工: 456人 | 本季度入职: 23人 | 离职: 8人</p>',
  },
  {
    id: 'tpl_monthly_marketing',
    name: '月度营销效果分析',
    description: '营销渠道ROI、获客成本、品牌曝光等营销效果分析报告',
    category: 'marketing',
    format: 'pptx',
    coverColor: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
    usageCount: 678,
    version: 'v2.0',
    templateContent: '# 月度营销效果 {{month}}\n\n## 渠道概览\n{{channels}}\n\n## ROI分析\n{{roi}}\n\n## 获客成本\n{{cac}}\n\n## 品牌曝光\n{{brand}}',
    previewHtml: '<h1>月度营销效果分析</h1><p>总营销投入: ¥234,567 | 获客: 1,234人 | CAC: ¥190</p>',
  },
  {
    id: 'tpl_supply_chain',
    name: '供应链月度报告',
    description: '库存周转、供应商交付、物流效率等供应链核心指标',
    category: 'supply',
    format: 'pdf',
    coverColor: 'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)',
    usageCount: 345,
    version: 'v1.3',
    templateContent: '# 供应链月度报告 {{month}}\n\n## 库存概况\n{{inventory}}\n\n## 供应商交付\n{{delivery}}\n\n## 物流效率\n{{logistics}}\n\n## 成本分析\n{{cost}}',
    previewHtml: '<h1>供应链月度报告</h1><p>库存周转率: 8.5次/月</p><p>准时交付率: 96.7%</p>',
  },
]

const filteredTemplates = computed(() => {
  return presetTemplates.filter(t => {
    if (searchKeyword.value && !t.name.includes(searchKeyword.value) && !t.description.includes(searchKeyword.value)) return false
    if (selectedCategory.value && t.category !== selectedCategory.value) return false
    if (selectedFormat.value && t.format !== selectedFormat.value) return false
    return true
  })
})

function handlePreview(template: MarketTemplate) {
  previewTemplate.value = template
  previewVisible.value = true
}

function handleCopyTemplate() {
  if (!previewTemplate.value) return
  navigator.clipboard.writeText(previewTemplate.value.templateContent)
  message.success('模板内容已复制到剪贴板')
}

async function handleUseTemplate() {
  if (!previewTemplate.value) return
  try {
    await post('/report-templates', {
      name: previewTemplate.value.name,
      description: previewTemplate.value.description,
      format: previewTemplate.value.format,
      templateContent: previewTemplate.value.templateContent,
    })
    message.success('模板已添加到你的模板库')
    previewVisible.value = false
  } catch {
    message.error('添加失败')
  }
}
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 0; }

.template-card {
  transition: all 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  }
}

.template-cover {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  .template-icon {
    font-size: 40px;
    color: rgba(255, 255, 255, 0.9);
  }

  .template-format {
    position: absolute;
    top: 8px;
    right: 8px;
  }
}

.template-desc {
  font-size: 12px;
  color: #999;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

.template-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .template-usage {
    font-size: 12px;
    color: #999;
  }
}

.preview-content {
  .preview-body {
    padding: 16px;
    background: #fafafa;
    border-radius: 8px;
    font-size: 14px;
    line-height: 1.8;

    h1, h2 { margin-top: 12px; }
  }
}
</style>
