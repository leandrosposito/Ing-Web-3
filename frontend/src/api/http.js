import { useAuthStore } from '../stores/auth'
import { leerError } from './errores'

/**
 * Llama a la API del backend agregando el token. Devuelve el JSON de la respuesta.
 * Uso: const ordenes = await api('/api/v1/ordenes')
 *      await api('/api/v1/algo', { method: 'POST', body: { campo: 1 } })
 */
export async function api(ruta, { method = 'GET', body } = {}) {
  const auth = useAuthStore()
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth.token) headers.Authorization = `Bearer ${auth.token}`

  const res = await fetch(ruta, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!res.ok) {
    // 401 con sesion abierta = token vencido o invalido: se cierra la sesion (App.vue redirige al login)
    if (res.status === 401 && auth.token) auth.logout()
    throw await leerError(res)
  }
  return res.status === 204 ? null : res.json()
}
