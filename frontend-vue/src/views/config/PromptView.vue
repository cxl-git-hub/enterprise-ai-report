<template>
  <div class="page-container">
    <PageHeader title="提示词模板管理" subtitle="管理AI提示词模板">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建模板
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索模板名称" allow-clear>
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="searchParams.category" placeholder="分类" allow-clear style="width: 150px">
            <a-select-option value="nl2sql">NL2SQL</a-select-option>
            <a-select-option value="analysis">分析</a-select-option>
            <a-select-option value="report">报表</a-select-option>
            <a-select-option value="general">通用</a-select-option>
          </a-select>
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
          <template v-if="column.key === 'category'">
            <a-tag :color="categoryColor[record.category] || 'default'">{{ categoryLabel[record.category] || record.category }}</a-tag>
          </template>
          <template v-if="column.key === 'template'">
            <a-tooltip :title="record.template">
              <span class="template-preview">{{ truncate(record.template, 60) }}</span>
            </a-tooltip>
          </template>
          <template v-if="column.key === 'variables'">
            <a-tag v-for="v in (record.variables || [])" :key="v" size="small">{{ `{{${v}}}` }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
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
      :title="editingId ? '编辑提示词模板' : '新建提示词模板'"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      width="850px"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模板名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入名称">
                <template #tooltip><span>提示词模板的显示名称</span></template>
              </a-input>
              <div class="field-hint">示例：订单分析NL2SQL模板</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="分类" name="category">
              <a-select v-model:value="formState.category" placeholder="请选择分类">
                <a-select-option value="nl2sql">NL2SQL</a-select-option>
                <a-select-option value="analysis">分析</a-select-option>
                <a-select-option value="report">报表</a-select-option>
                <a-select-option value="general">通用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="请输入描述" />
        </a-form-item>
        <a-form-item label="模板内容" name="template">
          <template #tooltip><span>支持 {{variable}} 格式的变量占位符</span></template>
          <div class="code-editor">
            <a-textarea
              v-model:value="formState.template"
              :rows="10"
              placeholder="你是一个SQL专家。请根据以下Schema信息，将用户的自然语言查询转换为SQL。&#10;&#10;Schema:&#10;{{schema}}&#10;&#10;用户问题: {{question}}&#10;&#10;请生成SQL:"
              style="font-family: monospace; font-size: 13px; line-height: 1.6"
            />
          </div>
          <div class="field-hint">
            <a-button type="link" size="small" @click="aiSuggestTemplate">
              <ThunderboltOutlined /> AI优化模板
            </a-button>
          </div>
        </a-form-item>
        <a-form-item label="变量列表">
          <a-select
            v-model:value="formState.variables"
            mode="tags"
            placeholder="输入变量名后回车添加"
            :token-separators="[',']"
          />
          <div class="field-hint">从模板中自动提取的 {{variable}} 变量</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ConfirmDelete from '@/components/common/ConfirmDelete.vue'
import { useTable } from '@/composables/useTable'
import { useModal } from '@/composables/useModal'
import { promptApi, type Prompt, type PromptForm } from '@/api/prompt'
import { post } from '@/api/request'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()

const categoryColor: Record<string, string> = {
  nl2sql: 'blue',
  analysis: 'green',
  report: 'orange',
  general: 'default',
}

const categoryLabel: Record<string, string> = {
  nl2sql: 'NL2SQL',
  analysis: '分析',
  report: '报表',
  general: '通用',
}

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<Prompt>({
    fetchApi: (params) => promptApi.list(params as { page: number; pageSize: number; keyword?: string; category?: string }),
  })

const { visible: modalVisible, confirmLoading, editingId, openModal, handleOk } = useModal()

const formState = reactive<PromptForm>({
  name: '',
  description: '',
  template: '',
  variables: [],
  category: 'general',
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  template: [{ required: true, message: '请输入模板内容', trigger: 'blur' }],
}

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '分类', dataIndex: 'category', key: 'category', width: 100 },
  { title: '模板预览', dataIndex: 'template', key: 'template', ellipsis: true },
  { title: '变量', dataIndex: 'variables', key: 'variables', width: 200 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

function truncate(str: string, len: number) {
  return str && str.length > len ? str.slice(0, len) + '...' : str
}

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, { name: '', description: '', template: '', variables: [], category: 'general' })
  openModal()
}

async function openEditModal(record: Prompt) {
  openModal(record.id)
  const res = await promptApi.detail(record.id)
  Object.assign(formState, res.data)
}

function aiSuggestTemplate() {
  if (!formState.name) {
    message.warning('请先输入模板名称')
    return
  }
  message.loading({ content: 'AI正在优化提示词模板...', key: 'ai-optimize', duration: 0 })
  post<{ data: { template: string; explanation: string } }>('/ai/optimize-prompt', {
    name: formState.name,
    description: formState.description,
    category: formState.category,
    currentTemplate: formState.template,
  }).then((res) => {
    if (res?.data?.template) {
      formState.template = res.data.template
      // Auto-extract variables
      const matches = res.data.template.match(/\{\{(\w+)\}\}/g)
      if (matches) {
        formState.variables = [...new Set(matches.map((m: string) => m.replace(/[{}]/g, '')))]
      }
      message.success({ content: `AI已优化模板: ${res.data.explanation || '完成'}`, key: 'ai-optimize' })
    } else {
      message.info({ content: 'AI未能生成优化建议', key: 'ai-optimize' })
    }
  }).catch(() => {
    message.error({ content: 'AI优化服务暂时不可用', key: 'ai-optimize' })
  })
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  // Auto-extract variables from template
  const matches = formState.template.match(/\{\{(\w+)\}\}/g)
  if (matches) {
    formState.variables = [...new Set(matches.map((m: string) => m.replace(/[{}]/g, '')))]
  }
  await handleOk(async () => {
    if (editingId.value) {
      await promptApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await promptApi.create(formState)
      message.success('创建成功')
    }
    fetchData()
  })
}

async function handleDelete(id: string) {
  try {
    await promptApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch {}
}
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
.field-hint { font-size: 12px; color: #999; margin-top: 4px; }
.template-preview {
  font-size: 12px;
  color: #666;
  font-family: monospace;
}
</style>
