import { onMounted, onUnmounted } from 'vue'

/**
 * Ejecuta 'accion' apenas se muestra la pantalla y despues cada 'cadaMs' milisegundos.
 * Al salir de la pantalla se detiene. Asi se "actualiza sola" (polling).
 */
export function usePolling(accion, cadaMs) {
  let temporizador = null

  onMounted(() => {
    accion()
    temporizador = setInterval(accion, cadaMs)
  })

  onUnmounted(() => clearInterval(temporizador))
}
