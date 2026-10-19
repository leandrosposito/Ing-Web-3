/** Error de una llamada a la API: guarda el status HTTP y el mensaje que manda el backend. */
export class ApiError extends Error {
  constructor(status, mensaje) {
    super(mensaje)
    this.status = status
  }
}

/** El backend responde los errores con { status, mensaje, detalles }. Si no viene JSON, se usa un texto generico. */
export async function leerError(res) {
  let mensaje = `Error ${res.status}`
  try {
    const cuerpo = await res.json()
    if (cuerpo.mensaje) mensaje = cuerpo.mensaje
  } catch {
    // la respuesta no era JSON: se queda con el mensaje generico
  }
  return new ApiError(res.status, mensaje)
}
