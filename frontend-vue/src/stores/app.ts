import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const theme = ref<'light' | 'dark' | 'auto'>(
    (localStorage.getItem('theme') as 'light' | 'dark' | 'auto') || 'light'
  )
  const locale = ref(localStorage.getItem('locale') || 'zh-CN')
  const primaryColor = ref(localStorage.getItem('primaryColor') || '#1677ff')

  // Apply theme on init
  applyTheme(theme.value)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setTheme(t: 'light' | 'dark' | 'auto') {
    theme.value = t
    localStorage.setItem('theme', t)
    applyTheme(t)
  }

  function applyTheme(t: 'light' | 'dark' | 'auto') {
    let resolved = t
    if (t === 'auto') {
      resolved = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }
    document.documentElement.setAttribute('data-theme', resolved)
    document.documentElement.classList.toggle('dark', resolved === 'dark')
  }

  function setPrimaryColor(color: string) {
    primaryColor.value = color
    localStorage.setItem('primaryColor', color)
    document.documentElement.style.setProperty('--primary-color', color)
  }

  function setLocale(l: string) {
    locale.value = l
    localStorage.setItem('locale', l)
  }

  // Listen for system theme changes when in auto mode
  if (typeof window !== 'undefined') {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (theme.value === 'auto') {
        applyTheme('auto')
      }
    })
  }

  return { sidebarCollapsed, theme, locale, primaryColor, toggleSidebar, setTheme, setPrimaryColor, setLocale }
})
