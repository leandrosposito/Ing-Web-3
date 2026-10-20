import { defineStore } from 'pinia'
import { leerError } from '../api/errores'

const CLAVE = 'iw3_sesion'

/** Recupera la sesion guardada para seguir logueado al recargar la pagina. */
function leerSesion() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE)) ?? {}
  } catch {
    return {}
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const sesion = leerSesion()
    return {
      token: sesion.token ?? null,
      username: sesion.username ?? null,
      rol: sesion.rol ?? null,
    }
  },

  getters: {
    estaLogueado: (state) => !!state.token,
    // tieneRol('ADMIN', 'OPERADOR') -> true si el usuario tiene alguno de esos roles
    tieneRol: (state) => (...roles) => roles.includes(state.rol),
  },

  actions: {
    async login(username, password) {
      const res = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })
      if (!res.ok) throw await leerError(res)

      const datos = await res.json()
      this.token = datos.token
      this.username = datos.username
      this.rol = datos.rol
      localStorage.setItem(CLAVE, JSON.stringify(datos))
    },

    logout() {
      this.token = null
      this.username = null
      this.rol = null
      localStorage.removeItem(CLAVE)
    },
  },
})
