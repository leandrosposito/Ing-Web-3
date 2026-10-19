<script setup>
import { ref, watch } from 'vue'
import { api } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { usePolling } from '../composables/usePolling'
import { formatearFechaHora, formatearNumero } from '../utils/formato'

const auth = useAuthStore()

const estado = ref('PENDIENTE') // '' = todas
const alarmas = ref([])
const error = ref('')
// Observacion que se va escribiendo en cada alarma pendiente: { [idAlarma]: 'texto' }
const observaciones = ref({})

async function cargar() {
  const params = estado.value ? `?estado=${estado.value}` : ''
  try {
    alarmas.value = await api(`/api/v1/alarmas${params}`)
    error.value = ''
  } catch (e) {
    error.value = e.message
  }
}

async function aceptar(alarma) {
  try {
    await api(`/api/v1/alarmas/${alarma.id}/aceptar`, {
      method: 'POST',
      body: { observacion: observaciones.value[alarma.id] || null },
    })
    error.value = ''
    await cargar()
  } catch (e) {
    error.value = e.message
    await cargar() // por ejemplo, si otro usuario ya la habia aceptado
  }
}

watch(estado, cargar)
usePolling(cargar, 10000)
</script>

<template>
  <h1>Alarmas de temperatura</h1>

  <div class="filtros">
    <label>
      Mostrar
      <select v-model="estado">
        <option value="PENDIENTE">Pendientes</option>
        <option value="ACEPTADA">Aceptadas</option>
        <option value="">Todas</option>
      </select>
    </label>
  </div>

  <p v-if="error" class="error">{{ error }}</p>

  <table>
    <thead>
      <tr>
        <th>N°</th>
        <th>Orden</th>
        <th>Temperatura</th>
        <th>Umbral</th>
        <th>Fecha y hora</th>
        <th>Estado</th>
        <th>Aceptada por</th>
        <th>Fecha aceptación</th>
        <th>Observación</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="a in alarmas" :key="a.id">
        <td>{{ a.id }}</td>
        <td>{{ a.numeroOrden }}</td>
        <td>{{ formatearNumero(a.temperatura) }} °C</td>
        <td>{{ formatearNumero(a.umbral) }} °C</td>
        <td>{{ formatearFechaHora(a.fechaHora) }}</td>
        <td>{{ a.estado === 'PENDIENTE' ? 'Pendiente' : 'Aceptada' }}</td>
        <template v-if="a.estado === 'ACEPTADA'">
          <td>{{ a.usuarioAceptacion }}</td>
          <td>{{ formatearFechaHora(a.fechaAceptacion) }}</td>
          <td>{{ a.observacion ?? '-' }}</td>
        </template>
        <!-- Pendiente: solo OPERADOR y ADMIN pueden aceptarla -->
        <td v-else-if="auth.tieneRol('OPERADOR', 'ADMIN')" colspan="3">
          <input v-model="observaciones[a.id]" placeholder="Observación (opcional)" maxlength="500" />
          <button @click="aceptar(a)">Aceptar</button>
        </td>
        <td v-else colspan="3">Solo un operador o administrador puede aceptarla</td>
      </tr>
      <tr v-if="alarmas.length === 0">
        <td colspan="9">No hay alarmas para mostrar.</td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.filtros { display: flex; gap: 1rem; margin-bottom: 1rem; }
.filtros label { display: flex; flex-direction: column; gap: 0.25rem; }
td input { margin-right: 0.5rem; width: 60%; }
</style>
