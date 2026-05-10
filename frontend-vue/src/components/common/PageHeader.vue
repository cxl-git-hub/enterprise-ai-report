<template>
  <div class="page-header">
    <a-breadcrumb v-if="breadcrumbs && breadcrumbs.length" class="page-breadcrumb">
      <a-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
        <router-link v-if="item.path && index < breadcrumbs.length - 1" :to="item.path">
          {{ item.title }}
        </router-link>
        <span v-else>{{ item.title }}</span>
      </a-breadcrumb-item>
    </a-breadcrumb>
    <div class="page-header-row">
      <div class="page-header-info">
        <h2 class="page-title">{{ title }}</h2>
        <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
      </div>
      <div class="page-header-actions">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Breadcrumb {
  title: string
  path?: string
}

defineProps<{
  title: string
  subtitle?: string
  breadcrumbs?: Breadcrumb[]
}>()
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 24px;
}

.page-breadcrumb {
  margin-bottom: 12px;
}

.page-header-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

.page-subtitle {
  font-size: 14px;
  color: #888;
  margin: 6px 0 0;
  font-weight: 400;
}

.page-header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
