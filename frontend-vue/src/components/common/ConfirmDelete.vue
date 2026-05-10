<template>
  <a-popconfirm
    :title="title"
    :description="description"
    ok-text="确认删除"
    cancel-text="取消"
    ok-type="danger"
    @confirm="$emit('confirm')"
  >
    <slot />
  </a-popconfirm>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: '确认删除',
    description: '删除后不可恢复，是否继续？',
  }
)

defineEmits<{
  confirm: []
}>()
</script>
