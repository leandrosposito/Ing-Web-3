import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import InicioView from '../views/InicioView.vue'
import MonitoreoView from '../views/MonitoreoView.vue'
import AlarmasView from '../views/AlarmasView.vue'
import ConciliacionesView from '../views/ConciliacionesView.vue'

// Una ruta por linea. Las que no tienen "publica" exigen estar logueado.
const rutas = [
  { path: '/login', component: LoginView, meta: { publica: true } },
  { path: '/', component: InicioView },
  { path: '/monitoreo', component: MonitoreoView },
  { path: '/alarmas', component: AlarmasView },
  { path: '/conciliaciones', component: ConciliacionesView },
  { path: '/:pagina(.*)*', redirect: '/' },
]

export function crearRouter(history = createWebHistory()) {
  const router = createRouter({ history, routes: rutas })

  // Guard: se ejecuta antes de cada navegacion
  router.beforeEach((to) => {
    const auth = useAuthStore()
    if (!to.meta.publica && !auth.estaLogueado) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    if (to.path === '/login' && auth.estaLogueado) {
      return '/'
    }
  })

  return router
}
