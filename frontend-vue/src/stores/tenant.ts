import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { tenantApi, type Tenant } from '@/api/tenant'

export const useTenantStore = defineStore('tenant', () => {
  const currentTenant = ref<Tenant | null>(null)
  const tenantList = ref<Tenant[]>([])
  const loading = ref(false)

  const tenantId = computed(() => currentTenant.value?.id || '')

  async function fetchTenants() {
    loading.value = true
    try {
      const res = await tenantApi.list({ page: 1, pageSize: 100 })
      tenantList.value = res.data.items
      if (!currentTenant.value && tenantList.value.length > 0) {
        currentTenant.value = tenantList.value[0]
      }
    } finally {
      loading.value = false
    }
  }

  function setCurrentTenant(tenant: Tenant) {
    currentTenant.value = tenant
  }

  return { currentTenant, tenantList, tenantId, loading, fetchTenants, setCurrentTenant }
})
