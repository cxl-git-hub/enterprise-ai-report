/**
 * Global keyboard shortcuts
 */
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

interface Shortcut {
  key: string
  ctrl?: boolean
  shift?: boolean
  alt?: boolean
  action: () => void
  description: string
}

export function useKeyboardShortcuts() {
  const router = useRouter()
  const appStore = useAppStore()

  const shortcuts: Shortcut[] = [
    {
      key: 'k',
      ctrl: true,
      action: () => {
        const searchInput = document.querySelector('.header-right .ant-input-search input') as HTMLInputElement
        if (searchInput) {
          searchInput.focus()
          searchInput.select()
        }
      },
      description: '全局搜索',
    },
    {
      key: 'd',
      ctrl: true,
      action: () => router.push('/dashboard'),
      description: '返回仪表盘',
    },
    {
      key: 'b',
      ctrl: true,
      action: () => appStore.toggleSidebar(),
      description: '切换侧边栏',
    },
    {
      key: ',',
      ctrl: true,
      action: () => router.push('/settings'),
      description: '打开设置',
    },
    {
      key: 'd',
      ctrl: true,
      shift: true,
      action: () => {
        const newTheme = appStore.theme === 'dark' ? 'light' : 'dark'
        appStore.setTheme(newTheme)
      },
      description: '切换深色模式',
    },
  ]

  function handleKeydown(e: KeyboardEvent) {
    // Don't trigger in input/textarea
    const target = e.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
      return
    }

    for (const shortcut of shortcuts) {
      const ctrlMatch = shortcut.ctrl ? (e.ctrlKey || e.metaKey) : !(e.ctrlKey || e.metaKey)
      const shiftMatch = shortcut.shift ? e.shiftKey : !e.shiftKey
      const altMatch = shortcut.alt ? e.altKey : !e.altKey

      if (ctrlMatch && shiftMatch && altMatch && e.key.toLowerCase() === shortcut.key.toLowerCase()) {
        e.preventDefault()
        shortcut.action()
        return
      }
    }
  }

  function register() {
    window.addEventListener('keydown', handleKeydown)
  }

  function unregister() {
    window.removeEventListener('keydown', handleKeydown)
  }

  return { shortcuts, register, unregister, handleKeydown }
}
