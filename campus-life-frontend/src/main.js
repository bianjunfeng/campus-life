import { createApp } from 'vue'
import App from './App.vue'
import router from './router/index'
import './assets/global.css'
import './assets/admin-theme.css'
import { initializeAuthStorage } from './utils/authStorage'
import { installDialogPolyfill } from './utils/dialog'

const app = createApp(App)
initializeAuthStorage()
installDialogPolyfill()

app.use(router)

app.mount('#app')
