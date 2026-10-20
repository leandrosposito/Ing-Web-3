import { beforeEach, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/auth'
import AlarmasView from './AlarmasView.vue'

const CONFIG = { temperaturaUmbral: 40, destinatarios: ['uno@planta.local', 'dos@planta.local'] }

// fetch falso: registra las llamadas y responde segun la URL
function simularBackend() {
  const fetchFalso = vi.fn(async (url, opciones) => {
    const cuerpo = url.includes('/configuracion')
      ? (opciones.method === 'PUT' ? JSON.parse(opciones.body) : CONFIG)
      : []
    return { ok: true, status: 200, json: async () => cuerpo }
  })
  vi.stubGlobal('fetch', fetchFalso)
  return fetchFalso
}

function montarComo(rol) {
  const auth = useAuthStore()
  auth.token = 'token-falso'
  auth.rol = rol
  return mount(AlarmasView)
}

beforeEach(() => {
  setActivePinia(createPinia())
})

it('el admin ve la configuracion actual y puede guardarla', async () => {
  const fetchFalso = simularBackend()
  const pantalla = montarComo('ADMIN')
  await flushPromises()

  const [umbral, destinatarios] = pantalla.findAll('.config input')
  expect(umbral.element.value).toBe('40')
  expect(destinatarios.element.value).toBe('uno@planta.local, dos@planta.local')

  await umbral.setValue('55.5')
  await destinatarios.setValue('nuevo@planta.local, otro@planta.local')
  await pantalla.find('.config').trigger('submit')
  await flushPromises()

  const put = fetchFalso.mock.calls.find(([, op]) => op?.method === 'PUT')
  expect(put[0]).toBe('/api/v1/alarmas/configuracion')
  expect(JSON.parse(put[1].body)).toEqual({
    temperaturaUmbral: 55.5,
    destinatarios: ['nuevo@planta.local', 'otro@planta.local'],
  })
  expect(pantalla.text()).toContain('Configuración guardada')
})

it('un operador no ve el formulario ni pide la configuracion', async () => {
  const fetchFalso = simularBackend()
  const pantalla = montarComo('OPERADOR')
  await flushPromises()

  expect(pantalla.find('.config').exists()).toBe(false)
  expect(fetchFalso.mock.calls.some(([url]) => url.includes('/configuracion'))).toBe(false)
})
