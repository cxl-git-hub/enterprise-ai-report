<template>
  <div class="search-form">
    <a-form layout="inline" :model="formState" @finish="$emit('search')">
      <slot :form="formState" />
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit">
            <SearchOutlined /> 搜索
          </a-button>
          <a-button @click="$emit('reset')">
            <ReloadOutlined /> 重置
          </a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  fields: Record<string, unknown>
}>()

defineEmits<{
  search: []
  reset: []
}>()

const formState = reactive<Record<string, unknown>>({ ...props.fields })
</script>

<style lang="scss" scoped>
.search-form {
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}
</style>
