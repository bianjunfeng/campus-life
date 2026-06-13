import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import '../../src/assets/global.css'
import '../../src/assets/admin-theme.css'
import { initializeAuthStorage } from '../../src/shared/utils/authStorage'
import { startPresenceClient } from '../../src/shared/presence/presenceClient'
import { installDialogPolyfill } from '../../src/utils/dialog'

initializeAuthStorage()
installDialogPolyfill()
startPresenceClient('merchant')

createApp(App).use(router).mount('#app')
