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

// ---- Configuracion (solo ADMIN): umbral y destinatarios de los mails ----
const esAdmin = auth.tieneRol('ADMIN')
const config = ref({ temperaturaUmbral: null, destinatarios: '' }) // destinatarios: texto separado por coma
const configMensaje = ref('')
const configError = ref('')

async function cargarConfig() {
  try {
    const c = await api('/api/v1/alarmas/configuracion')
    config.value = { temperaturaUmbral: c.temperaturaUmbral, destinatarios: c.destinatarios.join(', ') }
  } catch (e) {
    configError.value = e.message
  }
}

async function guardarConfig() {
  configMensaje.value = ''
  configError.value = ''
  try {
    const c = await api('/api/v1/alarmas/configuracion', {
      method: 'PUT',
      body: {
        temperaturaUmbral: Number(config.value.temperaturaUmbral),
        destinatarios: config.value.destinatarios.split(',').map((m) => m.trim()).filter(Boolean),
      },
    })
    config.value = { temperaturaUmbral: c.temperaturaUmbral, destinatarios: c.destinatarios.join(', ') }
    configMensaje.value = 'Configuración guardada. Rige desde el próximo dato de carga.'
  } catch (e) {
    configError.value = e.message
  }
}

watch(estado, cargar)
usePolling(cargar, 10000)
if (esAdmin) cargarConfig()
</script>

<template>
  <h1>Alarmas de temperatura</h1>

  <!-- Solo ADMIN: el backend tambien lo exige (403 para el resto) -->
  <form v-if="esAdmin" class="config" @submit.prevent="guardarConfig">
    <h2>Configuración</h2>
    <label>
      Umbral de temperatura (°C)
      <input v-model="config.temperaturaUmbral" type="number" step="0.1" min="-50" max="200" required />
    </label>
    <label class="destinatarios">
      Destinatarios (mails separados por coma)
      <input v-model="config.destinatarios" type="text" required />
    </label>
    <button type="submit">Guardar</button>
    <p v-if="configMensaje" class="ok">{{ configMensaje }}</p>
    <p v-if="configError" class="error">{{ configError }}</p>
  </form>

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
.config { display: flex; flex-wrap: wrap; gap: 1rem; align-items: flex-end; margin-bottom: 1.5rem; padding: 1rem; background: #f4f5f7; border-radius: 6px; }
.config h2 { width: 100%; margin: 0; font-size: 1.1rem; }
.config label { display: flex; flex-direction: column; gap: 0.25rem; }
.config .destinatarios { flex: 1; min-width: 16rem; }
.config .destinatarios input { width: 100%; margin: 0; }
.config .ok { width: 100%; margin: 0; color: #1b7f3b; }
.config .error { width: 100%; margin: 0; }
</style>
