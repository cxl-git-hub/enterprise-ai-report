<template>
  <div class="page-container">
    <PageHeader title="数据源管理" subtitle="管理数据库连接">
      <template #actions>
        <a-button type="primary" @click="openCreateModal">
          <PlusOutlined /> 新建数据源
        </a-button>
      </template>
    </PageHeader>

    <a-card :bordered="false" class="page-card">
      <a-form layout="inline" :model="searchParams" class="search-bar" @finish="search">
        <a-form-item>
          <a-input v-model:value="searchParams.keyword" placeholder="搜索数据源名称" allow-clear>
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
          <template v-if="column.key === 'type'">
            <a-tag :color="typeColorMap[record.type] || 'default'">{{ record.type }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.lastTestStatus === 'success' ? 'success' : record.lastTestStatus === 'failed' ? 'error' : 'default'"
              :text="record.lastTestStatus === 'success' ? '连接正常' : record.lastTestStatus === 'failed' ? '连接失败' : '未测试'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <div class="table-actions">
              <a-button type="link" size="small" @click="handleTest(record.id)" :loading="testingId === record.id">
                测试连接
              </a-button>
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
      :title="editingId ? '编辑数据源' : '新建数据源'"
      :confirm-loading="confirmLoading"
      @ok="handleSubmit"
      width="650px"
    >
      <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="数据源名称" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入名称">
                <template #tooltip><span>数据源的显示名称</span></template>
              </a-input>
              <div class="field-hint">示例：生产数据库-主库</div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据库类型" name="type">
              <a-select v-model:value="formState.type" placeholder="请选择类型">
                <a-select-option value="mysql">MySQL</a-select-option>
                <a-select-option value="postgresql">PostgreSQL</a-select-option>
                <a-select-option value="clickhouse">ClickHouse</a-select-option>
                <a-select-option value="oracle">Oracle</a-select-option>
                <a-select-option value="sqlserver">SQL Server</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="16">
            <a-form-item label="主机地址" name="host">
              <a-input v-model:value="formState.host" placeholder="请输入主机地址" />
              <div class="field-hint">示例：192.168.1.100 或 db.example.com</div>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="端口" name="port">
              <a-input-number v-model:value="formState.port" :min="1" :max="65535" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="数据库名" name="database">
              <a-input v-model:value="formState.database" placeholder="请输入数据库名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="用户名" name="username">
              <a-input v-model:value="formState.username" placeholder="请输入用户名" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import ConfirmDelete from '@/components/common/ConfirmDelete.vue'
import { useTable } from '@/composables/useTable'
import { useModal } from '@/composables/useModal'
import { datasourceApi, type DataSource, type DataSourceForm } from '@/api/datasource'
import type { FormInstance } from 'ant-design-vue'

const formRef = ref<FormInstance>()
const testingId = ref<string | null>(null)

const typeColorMap: Record<string, string> = {
  mysql: 'blue',
  postgresql: 'green',
  clickhouse: 'orange',
  oracle: 'red',
  sqlserver: 'purple',
}

const { loading, dataSource, pagination, searchParams, fetchData, handleTableChange, search, resetSearch } =
  useTable<DataSource>({
    fetchApi: (params) => datasourceApi.list(params as { page: number; pageSize: number; keyword?: string }),
  })

const { visible: modalVisible, confirmLoading, editingId, openModal, closeModal, handleOk } = useModal()

const formState = reactive<DataSourceForm>({
  name: '',
  type: 'mysql',
  host: '',
  port: 3306,
  database: '',
  username: '',
  password: '',
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  database: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
}

const columns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: '主机', dataIndex: 'host', key: 'host' },
  { title: '端口', dataIndex: 'port', key: 'port', width: 80 },
  { title: '数据库', dataIndex: 'database', key: 'database' },
  { title: '连接状态', dataIndex: 'status', key: 'status', width: 120 },
  { title: '最后测试', dataIndex: 'lastTestAt', key: 'lastTestAt', width: 180 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' as const },
]

function openCreateModal() {
  editingId.value = null
  Object.assign(formState, { name: '', type: 'mysql', host: '', port: 3306, database: '', username: '', password: '' })
  openModal()
}

async function openEditModal(record: DataSource) {
  openModal(record.id)
  const res = await datasourceApi.detail(record.id)
  Object.assign(formState, res.data)
}

async function handleSubmit() {
  await formRef.value?.validateFields()
  await handleOk(async () => {
    if (editingId.value) {
      await datasourceApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await datasourceApi.create(formState)
      message.success('创建成功')
    }
    fetchData()
  })
}

async function handleTest(id: string) {
  testingId.value = id
  try {
    const res = await datasourceApi.testConnection(id)
    if (res.data.success) {
      message.success('连接测试成功')
    } else {
      message.error(`连接测试失败: ${res.data.message}`)
    }
    fetchData()
  } finally {
    testingId.value = null
  }
}

async function handleDelete(id: string) {
  try {
    await datasourceApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch {}
}
</script>

<style lang="scss" scoped>
.search-bar { margin-bottom: 16px; }
.field-hint { font-size: 12px; color: #999; margin-top: 4px; }
</style>
