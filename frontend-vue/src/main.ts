import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { useKeyboardShortcuts } from './composables/useKeyboardShortcuts'
import './assets/styles/main.scss'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(Antd)

// Register global keyboard shortcuts
const { register: registerShortcuts } = useKeyboardShortcuts()
registerShortcuts()

app.mount('#app')
