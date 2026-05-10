import { ref } from 'vue'

export function useModal() {
  const visible = ref(false)
  const confirmLoading = ref(false)
  const editingId = ref<string | null>(null)

  function openModal(id?: string) {
    editingId.value = id || null
    visible.value = true
  }

  function closeModal() {
    visible.value = false
    editingId.value = null
    confirmLoading.value = false
  }

  async function handleOk(callback: () => Promise<void>) {
    confirmLoading.value = true
    try {
      await callback()
      closeModal()
    } catch {
      // Error handled by caller
    } finally {
      confirmLoading.value = false
    }
  }

  return {
    visible,
    confirmLoading,
    editingId,
    openModal,
    closeModal,
    handleOk,
  }
}
