<script setup>
import { watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
const router = useRouter()

// Si la sesion se cierra (boton Salir o token vencido), se vuelve al login
watch(
  () => auth.estaLogueado,
  (logueado) => {
    if (!logueado) router.push('/login')
  },
)
</script>

<template>
  <header v-if="auth.estaLogueado" class="barra">
    <strong>IW3 · Ordenes de carga</strong>
    <nav>
      <RouterLink to="/">Inicio</RouterLink>
      <RouterLink to="/monitoreo">Monitoreo</RouterLink>
      <RouterLink to="/alarmas">Alarmas</RouterLink>
      <RouterLink to="/conciliaciones">Conciliaciones</RouterLink>
    </nav>
    <span class="usuario">{{ auth.username }} ({{ auth.rol }})</span>
    <button @click="auth.logout()">Salir</button>
  </header>
  <main>
    <RouterView />
  </main>
</template>
