<template>
  <div class="page-container">
    <PageHeader title="报表模板管理" subtitle="管理报表输出模板">
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
          <template v-if="column.key === 'format'">
            <a-tag :color="formatColor[record.format] || 'default'">{{ record.format.toUpperCase() }}</a-tag>
          </template>
          <template v-if="column.key === 'sections'">
            <a-tag v-for="sec in (record.sections || []).slice(0, 3)" :key="sec.name" size="small">
              {{ sec.name }}
            </a-tag>
            <a-tag v-if="(record.sections || []).length > 3" size="small">+{{ record.sections.length - 3 }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'active' ? 'success' : 'default'"
              :text="record.status === 'active' ? '已发布' : '草稿'"
            />
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
      :title="editingId ? '编辑报表模板' : '新建报表模板'"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      width="800px"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="模板名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入名称">
                <template #tooltip><span>报表模板的显示名称</span></template>
              </a-input>
              <div class="field-hint">示例：月度经营分析报告</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="输出格式" name="format">
              <a-select v-model:value="formState.format" placeholder="请选择格式">
                <a-select-option value="pdf">PDF</a-select-option>
                <a-select-option value="excel">Excel</a-select-option>
                <a-select-option value="html">HTML</a-select-option>
                <a-select-option value="docx">Word</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formState.description" :rows="2" placeholder="请输入描述" />
        </a-form-item>
        <a-form-item label="模板内容" name="templateContent">
          <template #tooltip><span>报表模板的HTML/Markdown内容</span></template>
          <div class="code-editor">
            <a-textarea
              v-model:value="formState.templateContent"
              :rows="10"
              placeholder="<h1>{{title}}</h1>&#10;<p>报告期间: {{period}}</p>&#10;<div>{{content}}</div>"
              style="font-family: monospace; font-size: 13px"
            />
          </div>
          <div class="field-hint">
            <a-button type="link" size="small" @click="aiSuggestTemplate">
              <ThunderboltOutlined /> AI生成模板
            </a-button>
          </div>
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
import { reportTemplateApi, type ReportTemplate, type ReportTemplateForm } from '@/api/report-template'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()

const formatColor: Record<string, string> = {
  pdf: 'red',
  excel: 'green',
  html: 'blue',
  docx: 'purple',
}

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<ReportTemplate>({
    fetchApi: (params) => reportTemplateApi.list(params as { page: number; pageSize: number; keyword?: string }),
  })

const { visible: modalVisible, confirmLoading, editingId, openModal, handleOk } = useModal()

const formState = reactive<ReportTemplateForm>({
  name: '',
  description: '',
  format: 'pdf',
  templateContent: '',
  sections: [],
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  format: [{ required: true, message: '请选择格式', trigger: 'change' }],
  templateContent: [{ required: true, message: '请输入模板内容', trigger: 'blur' }],
}

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '格式', dataIndex: 'format', key: 'format', width: 80 },
  { title: '章节', dataIndex: 'sections', key: 'sections', width: 200 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '更新时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 180 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, { name: '', description: '', format: 'pdf', templateContent: '', sections: [] })
  openModal()
}

async function openEditModal(record: ReportTemplate) {
  openModal(record.id)
  const res = await reportTemplateApi.detail(record.id)
  Object.assign(formState, res.data)
}

function aiSuggestTemplate() {
  message.info('AI生成功能需要连接后端AI服务')
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  await handleOk(async () => {
    if (editingId.value) {
      await reportTemplateApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await reportTemplateApi.create(formState)
      message.success('创建成功')
    }
    fetchData()
  })
}

async function handleDelete(id: string) {
  try {
    await reportTemplateApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch {}
}
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
.field-hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
