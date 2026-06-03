import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import '../../src/assets/global.css'
import { initializeAuthStorage } from '../../src/shared/utils/authStorage'
import { installDialogPolyfill } from '../../src/utils/dialog'

initializeAuthStorage()
installDialogPolyfill()

createApp(App).use(router).mount('#app')

