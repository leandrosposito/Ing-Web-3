import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { crearRouter } from './router'
import './style.css'

const app = createApp(App)
// Pinia tiene que estar instalado antes que el router: el guard del router usa el store
app.use(createPinia())
app.use(crearRouter())
app.mount('#app')
