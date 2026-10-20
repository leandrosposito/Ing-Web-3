import { onMounted, onUnmounted } from 'vue'

/** Ejecuta accion al mostrar la pantalla y despues cada cadaMs milisegundos (polling); se detiene al salir. */
export function usePolling(accion, cadaMs) {
  let temporizador = null

  onMounted(() => {
    accion()
    temporizador = setInterval(accion, cadaMs)
  })

  onUnmounted(() => clearInterval(temporizador))
}
