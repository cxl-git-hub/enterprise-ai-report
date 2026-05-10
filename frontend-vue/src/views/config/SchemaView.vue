<template>
  <div class="page-container">
    <PageHeader title="Schema管理" subtitle="管理数据Schema定义和版本">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建Schema
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索Schema名称" allow-clear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'version'">
            <a-tag color="blue">v{{ record.version }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'active' ? 'success' : 'default'"
              :text="record.status === 'active' ? '已发布' : '草稿'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="openVersionHistory(record)">版本历史</a-button>
              <a-button type="link" size="small" @click="openEditModal(record)">编辑</a-button>
              <ConfirmDelete @confirm="handleDelete(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </ConfirmDelete>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Form Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑Schema' : '新建Schema'"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      width="900px"
      :destroy-on-close="true"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="Schema名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入名称">
                <template #tooltip><span>Schema的显示名称，用于AI理解和引用</span></template>
              </a-input>
              <div class="field-hint">示例：订单数据Schema</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联数据集" name="datasetId">
              <a-select v-model:value="formState.datasetId" placeholder="请选择数据集">
                <a-select-option v-for="ds in datasetOptions" :key="ds.id" :value="ds.id">
                  {{ ds.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="请输入Schema描述">
            <template #tooltip><span>业务含义说明，帮助AI理解数据上下文</span></template>
          </a-textarea>
        </a-form-item>

        <a-form-item label="列定义">
          <template #tooltip><span>定义列的名称、类型、业务含义等信息</span></template>

          <!-- Structured Column Editor -->
          <div class="column-editor">
            <div class="column-toolbar">
              <a-space>
                <a-button size="small" type="primary" @click="addColumn">
                  <PlusOutlined /> 添加列
                </a-button>
                <a-button size="small" @click="aiSuggestColumns">
                  <ThunderboltOutlined /> AI建议
                </a-button>
                <a-button size="small" @click="toggleColumnJson">
                  <CodeOutlined /> {{ showColumnJson ? '表单' : 'JSON' }}
                </a-button>
              </a-space>
            </div>

            <!-- Visual Column List -->
            <div v-if="!showColumnJson" class="column-list">
              <div v-if="columnItems.length === 0" class="column-empty">
                <a-empty description="暂无列定义，点击上方按钮添加" :image-style="{ height: '30px' }" />
              </div>
              <div
                v-for="(col, idx) in columnItems"
                :key="idx"
                class="column-card"
              >
                <div class="column-card-header">
                  <a-tag :color="colTypeColors[col.type] || 'default'">{{ col.type || '未设置' }}</a-tag>
                  <span class="column-index">#{{ idx + 1 }}</span>
                  <a-space>
                    <a-button type="link" size="small" @click="moveColumn(idx, -1)" :disabled="idx === 0">↑</a-button>
                    <a-button type="link" size="small" @click="moveColumn(idx, 1)" :disabled="idx === columnItems.length - 1">↓</a-button>
                    <a-button type="link" size="small" danger @click="removeColumn(idx)">
                      <DeleteOutlined />
                    </a-button>
                  </a-space>
                </div>
                <a-row :gutter="8" style="margin-top: 8px">
                  <a-col :span="6">
                    <a-input v-model:value="col.name" size="small" placeholder="列名(英文)" @change="syncColumnsToJson" />
                  </a-col>
                  <a-col :span="4">
                    <a-select v-model:value="col.type" size="small" style="width: 100%" @change="syncColumnsToJson">
                      <a-select-option value="string">字符串</a-select-option>
                      <a-select-option value="number">数字</a-select-option>
                      <a-select-option value="date">日期</a-select-option>
                      <a-select-option value="boolean">布尔</a-select-option>
                      <a-select-option value="json">JSON</a-select-option>
                    </a-select>
                  </a-col>
                  <a-col :span="4">
                    <a-input v-model:value="col.description" size="small" placeholder="描述" @change="syncColumnsToJson" />
                  </a-col>
                  <a-col :span="6">
                    <a-input v-model:value="col.businessMeaning" size="small" placeholder="业务含义" @change="syncColumnsToJson" />
                  </a-col>
                  <a-col :span="4">
                    <a-space>
                      <a-checkbox v-model:checked="col.nullable" @change="syncColumnsToJson">可空</a-checkbox>
                      <a-checkbox v-model:checked="col.isPrimaryKey" @change="syncColumnsToJson">PK</a-checkbox>
                    </a-space>
                  </a-col>
                </a-row>
              </div>
            </div>

            <!-- JSON Fallback -->
            <div v-else class="code-editor">
              <a-textarea
                v-model:value="columnsJson"
                :rows="10"
                placeholder='[{"name": "id", "type": "bigint", "nullable": false, "description": "主键ID", "businessMeaning": "订单唯一标识"}]'
                style="font-family: monospace; font-size: 13px"
              />
            </div>
          </div>
        </a-form-item>

        <a-form-item label="变更说明">
          <a-input v-model:value="changeNote" placeholder="请输入本次变更说明" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Version History Modal -->
    <a-modal
      v-model:open="versionModalVisible"
      title="版本历史"
      :footer="null"
      width="800px"
    >
      <a-timeline>
        <a-timeline-item
          v-for="ver in versionHistory"
          :key="ver.version"
          :color="ver.version === currentSchema?.version ? 'blue' : 'gray'"
        >
          <div class="version-item">
            <div class="version-header">
              <a-tag :color="ver.version === currentSchema?.version ? 'blue' : 'default'">
                v{{ ver.version }}
              </a-tag>
              <span class="version-time">{{ ver.createdAt }}</span>
              <span class="version-user">{{ ver.createdBy }}</span>
            </div>
            <p class="version-note">{{ ver.changeNote || '无变更说明' }}</p>
            <div class="version-columns">
              <a-tag v-for="col in ver.columns?.slice(0, 5)" :key="col.name" size="small">
                {{ col.name }}: {{ col.type }}
              </a-tag>
              <a-tag v-if="(ver.columns || []).length > 5" size="small">
                +{{ (ver.columns || []).length - 5 }} more
              </a-tag>
            </div>
          </div>
        </a-timeline-item>
      </a-timeline>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ThunderboltOutlined, DeleteOutlined, CodeOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ConfirmDelete from '@/components/common/ConfirmDelete.vue'
import { useTable } from '@/composables/useTable'
import { useModal } from '@/composables/useModal'
import { schemaApi, type Schema, type SchemaForm, type SchemaVersion, type SchemaColumn } from '@/api/schema'
import { datasetApi, type Dataset } from '@/api/dataset'
import { post } from '@/api/request'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()
const versionModalVisible = ref(false)
const versionHistory = ref<SchemaVersion[]>([])
const currentSchema = ref<Schema | null>(null)
const datasetOptions = ref<Dataset[]>([])
const columnsJson = ref('')
const changeNote = ref('')
const showColumnJson = ref(false)

interface ColumnItem {
  name: string
  type: string
  nullable: boolean
  isPrimaryKey: boolean
  description: string
  businessMeaning: string
  example?: string
}

const columnItems = ref<ColumnItem[]>([])

const colTypeColors: Record<string, string> = {
  string: 'blue',
  number: 'green',
  date: 'orange',
  boolean: 'purple',
  json: 'cyan',
  bigint: 'green',
  varchar: 'blue',
  datetime: 'orange',
}

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<Schema>({
    fetchApi: (params) => schemaApi.list(params as { page: number; pageSize: number; keyword?: string }),
  })

const { visible: modalVisible, confirmLoading, editingId, openModal, handleOk } = useModal()

const formState = reactive<SchemaForm>({
  name: '',
  description: '',
  datasetId: '',
  columns: [],
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  datasetId: [{ required: true, message: '请选择数据集', trigger: 'change' }],
}

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '数据集', dataIndex: 'datasetName', key: 'datasetName' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' as const },
]

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, { name: '', description: '', datasetId: '', columns: [] })
  columnsJson.value = '[]'
  columnItems.value = []
  showColumnJson.value = false
  changeNote.value = ''
  openModal()
}

async function openEditModal(record: Schema) {
  openModal(record.id)
  const res = await schemaApi.detail(record.id)
  Object.assign(formState, res.data)
  const cols = res.data.columns || []
  columnsJson.value = JSON.stringify(cols, null, 2)
  columnItems.value = cols.map((c: any) => ({
    name: c.name || '',
    type: c.type || 'string',
    nullable: c.nullable !== false,
    isPrimaryKey: c.isPrimaryKey || false,
    description: c.description || '',
    businessMeaning: c.businessMeaning || '',
  }))
  showColumnJson.value = false
  changeNote.value = ''
}

async function openVersionHistory(record: Schema) {
  currentSchema.value = record
  try {
    const res = await schemaApi.getVersions(record.id)
    versionHistory.value = res.data
  } catch {}
  versionModalVisible.value = true
}

function addSampleColumn() {
  const sample: SchemaColumn = {
    name: 'column_name',
    type: 'varchar(255)',
    nullable: true,
    description: '列描述',
    example: '示例值',
    businessMeaning: '业务含义说明',
  }
  try {
    const arr = JSON.parse(columnsJson.value || '[]')
    arr.push(sample)
    columnsJson.value = JSON.stringify(arr, null, 2)
  } catch {
    columnsJson.value = JSON.stringify([sample], null, 2)
  }
}

function addColumn() {
  columnItems.value.push({
    name: '',
    type: 'string',
    nullable: true,
    isPrimaryKey: false,
    description: '',
    businessMeaning: '',
  })
  syncColumnsToJson()
}

function removeColumn(idx: number) {
  columnItems.value.splice(idx, 1)
  syncColumnsToJson()
}

function moveColumn(idx: number, direction: number) {
  const target = idx + direction
  if (target < 0 || target >= columnItems.value.length) return
  const temp = columnItems.value[idx]
  columnItems.value[idx] = columnItems.value[target]
  columnItems.value[target] = temp
  columnItems.value = [...columnItems.value] // trigger reactivity
  syncColumnsToJson()
}

function syncColumnsToJson() {
  columnsJson.value = JSON.stringify(columnItems.value, null, 2)
}

function toggleColumnJson() {
  if (showColumnJson.value) {
    // Switching from JSON to form - parse
    try {
      const parsed = JSON.parse(columnsJson.value || '[]')
      columnItems.value = parsed.map((c: any) => ({
        name: c.name || '',
        type: c.type || 'string',
        nullable: c.nullable !== false,
        isPrimaryKey: c.isPrimaryKey || false,
        description: c.description || '',
        businessMeaning: c.businessMeaning || '',
      }))
    } catch {
      message.error('JSON格式错误，无法切换到表单模式')
      return
    }
  }
  showColumnJson.value = !showColumnJson.value
}

function aiSuggestColumns() {
  if (!formState.name) {
    message.warning('请先输入Schema名称')
    return
  }
  message.loading({ content: 'AI正在分析并建议列定义...', key: 'ai-suggest', duration: 0 })
  post<{ data: { columns: SchemaColumn[] } }>('/ai/suggest-columns', {
    schemaName: formState.name,
    description: formState.description,
    datasetId: formState.datasetId,
  }).then((res) => {
    if (res?.data?.columns?.length) {
      columnsJson.value = JSON.stringify(res.data.columns, null, 2)
      message.success({ content: `AI建议了 ${res.data.columns.length} 个列定义`, key: 'ai-suggest' })
    } else {
      message.info({ content: 'AI未能生成建议，请手动定义', key: 'ai-suggest' })
    }
  }).catch(() => {
    message.error({ content: 'AI建议服务暂时不可用', key: 'ai-suggest' })
  })
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  try {
    formState.columns = JSON.parse(columnsJson.value || '[]')
  } catch {
    message.error('列定义JSON格式错误')
    return
  }
  await handleOk(async () => {
    if (editingId.value) {
      await schemaApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await schemaApi.create(formState)
      message.success('创建成功')
    }
    fetchData()
  })
}

async function handleDelete(id: string) {
  try {
    await schemaApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch {}
}

onMounted(async () => {
  try {
    const res = await datasetApi.list({ page: 1, pageSize: 100 })
    datasetOptions.value = res.data.items
  } catch {}
})
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
.field-hint { font-size: 12px; color: #999; margin-top: 4px; }

.column-editor {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.column-toolbar {
  padding: 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.column-list {
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.column-empty {
  padding: 24px;
}

.column-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.2s;

  &:hover {
    border-color: #91caff;
  }
}

.column-card-header {
  display: flex;
  align-items: center;
  gap: 8px;

  .column-index {
    color: #999;
    font-size: 12px;
    flex: 1;
  }
}

.version-item {
  .version-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
  .version-time { color: #999; font-size: 12px; }
  .version-user { color: #666; font-size: 12px; }
  .version-note { color: #666; font-size: 13px; margin: 4px 0; }
  .version-columns { margin-top: 4px; }
}
</style>
