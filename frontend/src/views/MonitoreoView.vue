<script setup>
import { reactive, ref, watch } from 'vue'
import { api } from '../api/http'
import { usePolling } from '../composables/usePolling'
import { formatearDuracion, formatearEta, formatearNumero, formatearFechaHora } from '../utils/formato'

const ESTADOS = [
  { valor: '', texto: 'Todos' },
  { valor: 'PENDIENTE_PESAJE_INICIAL', texto: '1 · Pendiente de pesaje inicial' },
  { valor: 'CON_PESAJE_INICIAL', texto: '2 · Cargando' },
  { valor: 'CERRADA_PARA_CARGA', texto: '3 · Cerrada para carga' },
  { valor: 'FINALIZADA', texto: '4 · Finalizada' },
]

// Columnas de la tabla. "clave" es lo que entiende el backend en ?ordenarPor= (sin clave = no ordenable).
const COLUMNAS = [
  { clave: 'numeroOrden', titulo: 'Orden' },
  { clave: 'estado', titulo: 'Estado' },
  { clave: 'patente', titulo: 'Camión' },
  { clave: 'preset', titulo: 'Preset (kg)' },
  { clave: 'masa', titulo: 'Carga actual (kg)' },
  { clave: 'temperatura', titulo: 'Temp. (°C)' },
  { clave: 'densidad', titulo: 'Densidad' },
  { clave: 'caudal', titulo: 'Caudal (kg/h)' },
  { titulo: 'ETA' },
  { titulo: 'Tiempo cargando' },
  { titulo: 'Último dato' },
]

const filtros = reactive({ estado: '', patente: '', ordenarPor: 'numeroOrden', direccion: 'asc' })
const ordenes = ref([])
const error = ref('')
const actualizado = ref(null)

async function cargar() {
  const params = new URLSearchParams({ ordenarPor: filtros.ordenarPor, direccion: filtros.direccion })
  if (filtros.estado) params.set('estado', filtros.estado)
  if (filtros.patente) params.set('patente', filtros.patente)
  try {
    ordenes.value = await api(`/api/v1/monitoreo?${params}`)
    error.value = ''
    actualizado.value = new Date().toLocaleTimeString('es-AR')
  } catch (e) {
    error.value = e.message
  }
}

// Click en un encabezado: ordena por esa columna; si ya estaba, invierte la direccion
function ordenarPor(clave) {
  if (!clave) return
  if (filtros.ordenarPor === clave) {
    filtros.direccion = filtros.direccion === 'asc' ? 'desc' : 'asc'
  } else {
    filtros.ordenarPor = clave
    filtros.direccion = 'asc'
  }
}

function flecha(clave) {
  if (filtros.ordenarPor !== clave) return ''
  return filtros.direccion === 'asc' ? ' ▲' : ' ▼'
}

// Al cambiar un filtro o el orden se recarga enseguida (y sigue la actualizacion automatica)
watch(filtros, cargar)
usePolling(cargar, 5000)
</script>

<template>
  <h1>Monitoreo de órdenes</h1>

  <div class="filtros">
    <label>
      Estado
      <select v-model="filtros.estado">
        <option v-for="e in ESTADOS" :key="e.valor" :value="e.valor">{{ e.texto }}</option>
      </select>
    </label>
    <label>
      Patente
      <input v-model.trim="filtros.patente" placeholder="Buscar..." />
    </label>
    <span class="actualizado">Se actualiza cada 5 s · última: {{ actualizado ?? '-' }}</span>
  </div>

  <p v-if="error" class="error">{{ error }}</p>

  <table>
    <thead>
      <tr>
        <th
          v-for="c in COLUMNAS"
          :key="c.titulo"
          :class="{ ordenable: c.clave }"
          @click="ordenarPor(c.clave)"
        >
          {{ c.titulo }}{{ flecha(c.clave) }}
        </th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="o in ordenes" :key="o.numeroOrden">
        <td>{{ o.numeroOrden }}</td>
        <td>{{ o.estadoNumero }} · {{ o.estadoDescripcion }}</td>
        <td>{{ o.patente }}</td>
        <td>{{ formatearNumero(o.preset, 0) }}</td>
        <td>{{ formatearNumero(o.ultimaMasaAcumulada, 0) }}</td>
        <td>{{ formatearNumero(o.ultimaTemperatura) }}</td>
        <td>{{ formatearNumero(o.ultimaDensidad, 3) }}</td>
        <td>{{ formatearNumero(o.ultimoCaudal, 0) }}</td>
        <td>{{ formatearEta(o.etaHoras) }}</td>
        <td>{{ formatearDuracion(o.tiempoTranscurridoSegundos) }}</td>
        <td>{{ formatearFechaHora(o.fechaUltimoDato) }}</td>
      </tr>
      <tr v-if="ordenes.length === 0">
        <td :colspan="COLUMNAS.length">No hay órdenes para mostrar.</td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.filtros { display: flex; gap: 1rem; align-items: end; margin-bottom: 1rem; flex-wrap: wrap; }
.filtros label { display: flex; flex-direction: column; gap: 0.25rem; }
.actualizado { color: #52606d; font-size: 0.85rem; }
.ordenable { cursor: pointer; user-select: none; }
</style>
