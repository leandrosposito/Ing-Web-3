/** El backend responde los errores con { mensaje }; si no viene JSON se usa un texto generico. */
export async function leerError(res) {
  let mensaje = `Error ${res.status}`
  try {
    const cuerpo = await res.json()
    if (cuerpo.mensaje) mensaje = cuerpo.mensaje
  } catch {
    // no era JSON
  }
  return new Error(mensaje)
}
