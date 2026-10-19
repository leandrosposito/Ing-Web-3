<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api/http'
import { formatearFechaHora, formatearNumero } from '../utils/formato'

const ordenes = ref([]) // ordenes finalizadas (estado 4)
const seleccionada = ref(null) // numero de la orden elegida
const conciliacion = ref(null)
const error = ref('')

async function cargarOrdenes() {
  try {
    ordenes.value = await api('/api/v1/ordenes?estado=FINALIZADA')
    error.value = ''
  } catch (e) {
    error.value = e.message
  }
}

async function elegir(orden) {
  seleccionada.value = orden.numeroOrden
  conciliacion.value = null
  try {
    conciliacion.value = await api(`/api/v1/ordenes/${orden.numeroOrden}/conciliacion`)
    error.value = ''
  } catch (e) {
    error.value = e.message
  }
}

onMounted(cargarOrdenes)
</script>

<template>
  <h1>Conciliaciones</h1>
  <p class="ayuda">Órdenes finalizadas (estado 4). Elegí una para ver su conciliación.</p>
  <p v-if="error" class="error">{{ error }}</p>

  <div class="paneles">
    <section>
      <button @click="cargarOrdenes">Actualizar lista</button>
      <table class="lista">
        <thead>
          <tr><th>Orden</th><th>Camión</th><th>Cliente</th><th>Pesaje final</th></tr>
        </thead>
        <tbody>
          <tr
            v-for="o in ordenes"
            :key="o.numeroOrden"
            :class="{ elegida: o.numeroOrden === seleccionada }"
            @click="elegir(o)"
          >
            <td>{{ o.numeroOrden }}</td>
            <td>{{ o.camion.patente }}</td>
            <td>{{ o.cliente.razonSocial }}</td>
            <td>{{ formatearFechaHora(o.fechaPesajeFinal) }}</td>
          </tr>
          <tr v-if="ordenes.length === 0">
            <td colspan="4">No hay órdenes finalizadas.</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="conciliacion" class="detalle">
      <h2>Orden {{ conciliacion.numeroOrden }}</h2>
      <table>
        <tbody>
          <tr><th>Pesaje inicial (tara)</th><td>{{ formatearNumero(conciliacion.pesajeInicial) }} kg</td></tr>
          <tr><th>Pesaje final</th><td>{{ formatearNumero(conciliacion.pesajeFinal) }} kg</td></tr>
          <tr><th>Producto cargado (caudalímetro)</th><td>{{ formatearNumero(conciliacion.productoCargado) }} kg</td></tr>
          <tr><th>Neto por balanza</th><td>{{ formatearNumero(conciliacion.netoPorBalanza) }} kg</td></tr>
          <tr class="diferencia">
            <th>Diferencia (balanza − caudalímetro)</th>
            <td>{{ formatearNumero(conciliacion.diferenciaBalanzaCaudalimetro) }} kg</td>
          </tr>
          <tr><th>Temperatura promedio</th><td>{{ formatearNumero(conciliacion.promedioTemperatura) }} °C</td></tr>
          <tr><th>Densidad promedio</th><td>{{ formatearNumero(conciliacion.promedioDensidad, 3) }}</td></tr>
          <tr><th>Caudal promedio</th><td>{{ formatearNumero(conciliacion.promedioCaudal) }} kg/h</td></tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
.ayuda { color: #52606d; }
.paneles { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; align-items: start; }
.lista tbody tr { cursor: pointer; }
.lista tbody tr:hover { background: #eef3f8; }
.lista tbody tr.elegida { background: #dbe8f6; }
.detalle th { width: 55%; background: #f4f5f7; }
.diferencia td, .diferencia th { font-weight: 700; }
button { margin-bottom: 0.75rem; }
@media (max-width: 800px) { .paneles { grid-template-columns: 1fr; } }
</style>
