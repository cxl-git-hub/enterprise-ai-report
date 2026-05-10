import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const theme = ref<'light' | 'dark'>('light')
  const locale = ref('zh-CN')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setTheme(t: 'light' | 'dark') {
    theme.value = t
    document.documentElement.setAttribute('data-theme', t)
  }

  function setLocale(l: string) {
    locale.value = l
  }

  return { sidebarCollapsed, theme, locale, toggleSidebar, setTheme, setLocale }
})
