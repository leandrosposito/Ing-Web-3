/** Funciones para mostrar los datos en las pantallas. Todas devuelven "-" si no hay dato. */

const SIN_DATO = '-'

/** 3725 segundos -> "01:02:05" */
export function formatearDuracion(segundos) {
  if (segundos === null || segundos === undefined) return SIN_DATO
  const dos = (n) => String(n).padStart(2, '0')
  const horas = Math.floor(segundos / 3600)
  const minutos = Math.floor((segundos % 3600) / 60)
  return `${dos(horas)}:${dos(minutos)}:${dos(segundos % 60)}`
}

/** 1.5 horas -> "1 h 30 min" */
export function formatearEta(horas) {
  if (horas === null || horas === undefined) return SIN_DATO
  const minutosTotales = Math.round(horas * 60)
  const h = Math.floor(minutosTotales / 60)
  const m = minutosTotales % 60
  return h > 0 ? `${h} h ${m} min` : `${m} min`
}

/** Numero con 'decimales' decimales (por defecto 1). */
export function formatearNumero(valor, decimales = 1) {
  if (valor === null || valor === undefined) return SIN_DATO
  return Number(valor).toFixed(decimales)
}

/** "2026-11-10T08:30:00" -> fecha y hora en formato local. */
export function formatearFechaHora(iso) {
  if (!iso) return SIN_DATO
  return new Date(iso).toLocaleString('es-AR')
}
