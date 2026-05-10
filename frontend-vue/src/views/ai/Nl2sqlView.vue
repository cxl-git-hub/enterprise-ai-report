<template>
  <div class="page-container">
    <PageHeader title="NL2SQL Playground" subtitle="自然语言转SQL查询" />

    <a-row :gutter="16">
      <!-- Schema Context Panel -->
      <a-col :span="6">
        <a-card title="Schema上下文" :bordered="false" class="page-card" size="small">
          <a-select
            v-model:value="selectedSchemaId"
            placeholder="选择Schema"
            style="width: 100%; margin-bottom: 12px"
            @change="handleSchemaChange"
          >
            <a-select-option v-for="s in schemas" :key="s.id" :value="s.id">
              {{ s.name }}
            </a-select-option>
          </a-select>

          <div v-if="currentSchema" class="schema-tree">
            <div class="schema-name">
              <DatabaseOutlined /> {{ currentSchema.datasetName }}
            </div>
            <div v-for="col in currentSchema.columns" :key="col.name" class="schema-column">
              <div class="col-info">
                <span class="col-name">{{ col.name }}</span>
                <a-tag size="small" color="blue">{{ col.type }}</a-tag>
              </div>
              <div class="col-desc">{{ col.businessMeaning || col.description }}</div>
            </div>
          </div>
          <a-empty v-else description="请选择Schema" :image-style="{ height: '40px' }" />
        </a-card>
      </a-col>

      <!-- Main Content -->
      <a-col :span="18">
        <!-- Query Input -->
        <a-card :bordered="false" class="page-card" size="small">
          <div class="query-input">
            <a-textarea
              v-model:value="naturalQuery"
              :rows="3"
              placeholder="请输入自然语言查询，例如：查询上个月每个产品的销售总额，按金额降序排列"
              @pressEnter.ctrl="handleGenerate"
            />
            <div class="query-actions">
              <a-space>
                <a-button @click="handleValidate" :loading="validating">
                  <CheckCircleOutlined /> 验证SQL
                </a-button>
                <a-button type="primary" @click="handleGenerate" :loading="generating">
                  <ThunderboltOutlined /> 生成SQL
                </a-button>
                <a-button @click="handleExecute" :loading="executing" :disabled="!generatedSql">
                  <PlayCircleOutlined /> 执行查询
                </a-button>
              </a-space>
            </div>
          </div>
        </a-card>

        <!-- Generated SQL -->
        <a-card title="生成的SQL" :bordered="false" class="page-card" size="small" style="margin-top: 16px">
          <div v-if="generatedSql" class="sql-display">
            <div class="sql-toolbar">
              <a-space>
                <a-button size="small" @click="handleCopySql">
                  <CopyOutlined /> 复制
                </a-button>
                <a-button size="small" @click="handleFormatSql">
                  <FormatPainterOutlined /> 格式化
                </a-button>
              </a-space>
            </div>
            <pre class="sql-code"><code>{{ generatedSql }}</code></pre>
          </div>
          <a-empty v-else description="输入自然语言后点击「生成SQL」" :image-style="{ height: '40px' }" />
        </a-card>

        <!-- Validation Results -->
        <a-card
          v-if="validationResult"
          title="验证结果"
          :bordered="false"
          class="page-card"
          size="small"
          style="margin-top: 16px"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-alert
              :type="validationResult.valid ? 'success' : 'error'"
              :message="validationResult.valid ? 'SQL验证通过' : 'SQL验证失败'"
              show-icon
            />
            <div v-if="validationResult.errors?.length">
              <div v-for="(err, idx) in validationResult.errors" :key="idx" class="validation-error">
                <CloseCircleOutlined style="color: #ff4d4f" /> {{ err }}
              </div>
            </div>
          </a-space>
        </a-card>

        <!-- Query Results -->
        <a-card
          v-if="queryResults.length > 0"
          title="查询结果"
          :bordered="false"
          class="page-card"
          size="small"
          style="margin-top: 16px"
        >
          <a-table
            :columns="resultColumns"
            :data-source="queryResults"
            :pagination="{ pageSize: 20 }"
            size="small"
            :scroll="{ x: true }"
          />
        </a-card>

        <!-- Execution Trace -->
        <a-card :bordered="false" class="page-card" size="small" style="margin-top: 16px">
          <a-collapse>
            <a-collapse-panel key="trace" header="执行追踪">
              <div v-if="trace" class="trace-info">
                <a-descriptions :column="3" bordered size="small">
                  <a-descriptions-item label="模型">{{ trace.modelName }}</a-descriptions-item>
                  <a-descriptions-item label="Prompt Tokens">{{ trace.promptTokens }}</a-descriptions-item>
                  <a-descriptions-item label="Completion Tokens">{{ trace.completionTokens }}</a-descriptions-item>
                  <a-descriptions-item label="总Tokens">{{ trace.totalTokens }}</a-descriptions-item>
                  <a-descriptions-item label="耗时">{{ trace.duration }}ms</a-descriptions-item>
                  <a-descriptions-item label="费用">${{ (trace.cost || 0).toFixed(4) }}</a-descriptions-item>
                </a-descriptions>
              </div>
              <a-empty v-else description="生成SQL后显示追踪信息" :image-style="{ height: '40px' }" />
            </a-collapse-panel>
          </a-collapse>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  DatabaseOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  PlayCircleOutlined,
  CopyOutlined,
  FormatPainterOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { schemaApi, type Schema } from '@/api/schema'
import { post } from '@/api/request'

const schemas = ref<Schema[]>([])
const selectedSchemaId = ref<string>('')
const currentSchema = ref<Schema | null>(null)
const naturalQuery = ref('')
const generatedSql = ref('')
const validating = ref(false)
const generating = ref(false)
const executing = ref(false)
const validationResult = ref<{ valid: boolean; errors?: string[] } | null>(null)
const queryResults = ref<Record<string, unknown>[]>([])
const resultColumns = ref<Array<{ title: string; dataIndex: string; key: string }>>([])
const trace = ref<{
  modelName: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  duration: number
  cost: number
} | null>(null)

async function handleSchemaChange(schemaId: string) {
  const schema = schemas.value.find((s) => s.id === schemaId)
  if (schema) {
    try {
      const res = await schemaApi.detail(schemaId)
      currentSchema.value = res.data
    } catch {}
  }
}

async function handleGenerate() {
  if (!naturalQuery.value.trim()) {
    message.warning('请输入自然语言查询')
    return
  }
  if (!selectedSchemaId.value) {
    message.warning('请先选择Schema')
    return
  }
  generating.value = true
  try {
    const res = await post<{ data: { sql: string; trace: unknown } }>('/ai/nl2sql', {
      question: naturalQuery.value,
      schemaId: selectedSchemaId.value,
    })
    generatedSql.value = res.data.sql
    if (res.data.trace) {
      trace.value = res.data.trace as typeof trace.value
    }
    validationResult.value = null
    queryResults.value = []
  } finally {
    generating.value = false
  }
}

async function handleValidate() {
  if (!generatedSql.value) {
    message.warning('请先生成SQL')
    return
  }
  validating.value = true
  try {
    const res = await post<{ data: { valid: boolean; errors?: string[] } }>('/ai/nl2sql/validate', {
      sql: generatedSql.value,
      schemaId: selectedSchemaId.value,
    })
    validationResult.value = res.data
  } finally {
    validating.value = false
  }
}

async function handleExecute() {
  if (!generatedSql.value) return
  executing.value = true
  try {
    const res = await post<{ data: { columns: string[]; rows: Record<string, unknown>[] } }>('/ai/nl2sql/execute', {
      sql: generatedSql.value,
      schemaId: selectedSchemaId.value,
    })
    resultColumns.value = (res.data.columns || []).map((col: string) => ({
      title: col,
      dataIndex: col,
      key: col,
    }))
    queryResults.value = res.data.rows || []
  } finally {
    executing.value = false
  }
}

function handleCopySql() {
  navigator.clipboard.writeText(generatedSql.value)
  message.success('已复制到剪贴板')
}

function handleFormatSql() {
  // Simple SQL formatting
  const sql = generatedSql.value
  const formatted = sql
    .replace(/\bSELECT\b/gi, '\nSELECT')
    .replace(/\bFROM\b/gi, '\nFROM')
    .replace(/\bWHERE\b/gi, '\nWHERE')
    .replace(/\bAND\b/gi, '\n  AND')
    .replace(/\bOR\b/gi, '\n  OR')
    .replace(/\bGROUP BY\b/gi, '\nGROUP BY')
    .replace(/\bORDER BY\b/gi, '\nORDER BY')
    .replace(/\bHAVING\b/gi, '\nHAVING')
    .replace(/\bJOIN\b/gi, '\nJOIN')
    .replace(/\bLEFT JOIN\b/gi, '\nLEFT JOIN')
    .replace(/\bRIGHT JOIN\b/gi, '\nRIGHT JOIN')
    .trim()
  generatedSql.value = formatted
}

onMounted(async () => {
  try {
    const res = await schemaApi.list({ page: 1, pageSize: 100 })
    schemas.value = res.data.items
  } catch {}
})
</script>

<style lang="scss" scoped>
.schema-tree {
  .schema-name {
    font-weight: 600;
    padding: 8px 0;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 8px;
  }

  .schema-column {
    padding: 6px 0;
    border-bottom: 1px solid #fafafa;

    .col-info {
      display: flex;
      align-items: center;
      gap: 8px;

      .col-name { font-weight: 500; font-size: 13px; }
    }

    .col-desc { font-size: 11px; color: #999; margin-top: 2px; }
  }
}

.query-input {
  .query-actions {
    margin-top: 12px;
    display: flex;
    justify-content: flex-end;
  }
}

.sql-display {
  .sql-toolbar {
    margin-bottom: 8px;
    display: flex;
    justify-content: flex-end;
  }

  .sql-code {
    background: #1e1e1e;
    color: #d4d4d4;
    padding: 16px;
    border-radius: 8px;
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 14px;
    line-height: 1.6;
    white-space: pre-wrap;
    overflow-x: auto;
  }
}

.validation-error {
  padding: 4px 0;
  font-size: 13px;
}

.trace-info {
  margin-top: 8px;
}
</style>
