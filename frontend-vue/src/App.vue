<template>
  <a-config-provider :locale="zhCN">
    <router-view />
    <a-float-button
      v-if="showBackTop"
      :shape="'circle'"
      @click="scrollToTop"
      style="bottom: 24px; right: 24px"
    >
      <template #icon><VerticalAlignTopOutlined /></template>
    </a-float-button>
  </a-config-provider>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { VerticalAlignTopOutlined } from '@ant-design/icons-vue'

const showBackTop = ref(false)

function handleScroll() {
  showBackTop.value = window.scrollY > 300
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => window.addEventListener('scroll', handleScroll))
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>
