import { ref, reactive, onMounted, watch } from 'vue'
import type { Ref } from 'vue'

interface Pagination {
  current: number
  pageSize: number
  total: number
  showSizeChanger: boolean
  showQuickJumper: boolean
  showTotal: (total: number) => string
}

interface UseTableOptions<T> {
  fetchApi: (params: Record<string, unknown>) => Promise<{ data: { items: T[]; total: number } }>
  defaultPageSize?: number
  immediate?: boolean
}

export function useTable<T extends Record<string, any>>(options: UseTableOptions<T>) {
  const { fetchApi, defaultPageSize = 10, immediate = true } = options

  const loading = ref(false)
  const dataSource: Ref<T[]> = ref([]) as Ref<T[]>
  const searchParams = reactive<Record<string, unknown>>({})

  const pagination = reactive<Pagination>({
    current: 1,
    pageSize: defaultPageSize,
    total: 0,
    showSizeChanger: true,
    showQuickJumper: true,
    showTotal: (total: number) => `共 ${total} 条`,
  })

  async function fetchData() {
    loading.value = true
    try {
      const params: Record<string, unknown> = {
        page: pagination.current,
        pageSize: pagination.pageSize,
        size: pagination.pageSize,  // Backend uses 'size', not 'pageSize'
      }
      // Merge search params, removing empty values
      Object.entries(searchParams).forEach(([key, value]) => {
        if (value !== '' && value !== undefined && value !== null && value !== 'undefined') {
          params[key] = value
        }
      })
      const res = await fetchApi(params)
      dataSource.value = res.data.items
      pagination.total = res.data.total
    } catch {
      // Error handled by interceptor
    } finally {
      loading.value = false
    }
  }

  function handleTableChange(pag: { current?: number; pageSize?: number }) {
    if (pag.current) pagination.current = pag.current
    if (pag.pageSize) pagination.pageSize = pag.pageSize
    fetchData()
  }

  function search(extraParams?: Record<string, unknown>) {
    if (extraParams) {
      Object.assign(searchParams, extraParams)
    }
    pagination.current = 1
    fetchData()
  }

  function resetSearch() {
    Object.keys(searchParams).forEach((key) => {
      searchParams[key] = undefined
    })
    pagination.current = 1
    fetchData()
  }

  if (immediate) {
    onMounted(fetchData)
  }

  return {
    loading,
    dataSource,
    pagination,
    searchParams,
    fetchData,
    handleTableChange,
    search,
    resetSearch,
  }
}
