import { beforeEach, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ConciliacionesView from './ConciliacionesView.vue'

const ORDEN = {
  numeroOrden: 5001,
  camion: { patente: 'AA000AA' },
  cliente: { razonSocial: 'Cliente Test' },
  fechaPesajeFinal: '2026-11-10T10:00:00',
}
const CONCILIACION = {
  numeroOrden: 5001,
  pesajeInicial: 15000,
  pesajeFinal: 18100,
  productoCargado: 3000,
  netoPorBalanza: 3100,
  diferenciaBalanzaCaudalimetro: 100,
  promedioTemperatura: 20,
  promedioDensidad: 0.6,
  promedioCaudal: 60000,
}

// fetch falso: devuelve la lista de ordenes o la conciliacion segun la URL pedida
function simularBackend() {
  const fetchFalso = vi.fn(async (url) => {
    const cuerpo = url.includes('/conciliacion') ? CONCILIACION : [ORDEN]
    return { ok: true, status: 200, json: async () => cuerpo }
  })
  vi.stubGlobal('fetch', fetchFalso)
  return fetchFalso
}

beforeEach(() => {
  setActivePinia(createPinia())
})

it('lista solo las ordenes finalizadas', async () => {
  const fetchFalso = simularBackend()
  const pantalla = mount(ConciliacionesView)
  await flushPromises()

  expect(fetchFalso.mock.calls[0][0]).toBe('/api/v1/ordenes?estado=FINALIZADA')
  expect(pantalla.text()).toContain('5001')
  expect(pantalla.text()).toContain('AA000AA')
})

it('al elegir una orden muestra su conciliacion', async () => {
  simularBackend()
  const pantalla = mount(ConciliacionesView)
  await flushPromises()

  await pantalla.find('tbody tr').trigger('click')
  await flushPromises()

  const texto = pantalla.text()
  expect(texto).toContain('Orden 5001')
  expect(texto).toContain('15000.0 kg')   // tara
  expect(texto).toContain('3100.0 kg')    // neto por balanza
  expect(texto).toContain('100.0 kg')     // diferencia
})
