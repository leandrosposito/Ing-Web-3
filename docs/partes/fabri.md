# Parte de Fabri: carga en tiempo real (monitoreo y alarmas)

Rama `fabri`. Agrega la pantalla de monitoreo que se actualiza sola, el ETA de llenado, el tiempo transcurrido y las alarmas de temperatura por mail con su pantalla de gestión.

## Qué se implementó

### Backend (`backend/src/main/java/ar/edu/iua/iw3/`)

| Archivo | Qué hace |
|---|---|
| `controller/MonitoreoController.java` | `GET /api/v1/monitoreo` con filtros `estado` y `patente` y orden `ordenarPor` / `direccion`. |
| `service/MonitoreoService.java` | Arma cada fila con los últimos valores de la orden. Traduce `ordenarPor` a una columna permitida. |
| `service/CalculosMonitoreo.java` | Las dos cuentas: ETA y tiempo transcurrido. Son funciones puras. |
| `repository/OrdenRepository.java` | `buscarParaMonitoreo`: consulta con filtros opcionales y `Sort`. |
| `model/Alarma.java`, `model/EstadoAlarma.java`, `repository/AlarmaRepository.java` | La alarma (temperatura, umbral, fecha, estado, quién la aceptó, cuándo y observación). |
| `service/AlarmaService.java` | Decide si hay que alarmar (leyendo el umbral vigente), lista y acepta alarmas. |
| `model/ConfiguracionAlarma.java`, `repository/ConfiguracionAlarmaRepository.java` | Umbral y destinatarios guardados en la base (una sola fila, id 1). |
| `service/ConfiguracionAlarmaService.java` | `obtener()` (la crea con los valores de las variables si no existe) y `actualizar()` (guarda quién y cuándo). |
| `dto/ConfiguracionAlarmaRequest.java`, `dto/ConfiguracionAlarmaResponse.java` | Entrada validada (umbral -50 a 200, 1 a 10 mails válidos) y salida. |
| `service/AlarmaCreada.java` | Evento que se publica al crear una alarma; lleva los destinatarios vigentes. |
| `service/NotificadorAlarmas.java` | Escucha el evento y manda el mail (después del commit). |
| `controller/AlarmaController.java` | `GET /api/v1/alarmas`, `POST /api/v1/alarmas/{id}/aceptar` y `GET`/`PUT /api/v1/alarmas/configuracion`. |
| `service/CargaService.java` | Después de guardar un dato válido llama a `alarmaService.evaluar(...)`. |
| `security/SecurityConfig.java` | Aceptar una alarma: solo `OPERADOR` y `ADMIN`. Ver o cambiar la configuración: solo `ADMIN`. |
| `application.properties` | Valores **iniciales** de umbral y destinatarios, remitente y servidor de mail. |

### Frontend (`frontend/src/`)

| Archivo | Qué hace |
|---|---|
| `views/MonitoreoView.vue` | Tabla de órdenes con filtros, orden por click en el encabezado y actualización cada 5 segundos. |
| `views/AlarmasView.vue` | Lista de alarmas (pendientes por defecto), botón Aceptar con observación y, solo para `ADMIN`, el formulario de configuración (umbral y mails). |
| `composables/usePolling.js` | Repite una función cada N ms mientras la pantalla está abierta. |
| `utils/formato.js` (+ test) | Formato de duración (`hh:mm:ss`), ETA (`1 h 30 min`), números y fechas. |
| `router/index.js`, `App.vue` | Rutas `/monitoreo` y `/alarmas` y los links del menú. |

### Configuración

| Propiedad | Variable de entorno | Por defecto |
|---|---|---|
| `app.alarma.temperatura-umbral` | `ALARMA_TEMPERATURA_UMBRAL` | `40` (grados) |
| `app.alarma.destinatarios` | `ALARMA_DESTINATARIOS` | `operaciones@planta.local` (varios, separados por coma) |
| `app.alarma.remitente` | `ALARMA_REMITENTE` | `alarmas@planta.local` |
| `spring.mail.host` / `spring.mail.port` | `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` (Mailpit) |

## Cómo funciona

**ETA y tiempo transcurrido (solo estado 2)**
- ETA en horas = `(preset − masa acumulada) / caudal`. Si la masa ya pasó el preset, es 0. Sin caudal válido, no se informa.
- Tiempo transcurrido = ahora − `fechaInicioCarga` (el primer dato válido). Se devuelve en segundos y el frontend lo muestra como `hh:mm:ss`.

**Alarma de temperatura**
1. Llega un dato de carga y pasa las reglas de descarte (los descartados nunca alarman).
2. Si `temperatura > umbral` y la orden **no tiene** una alarma pendiente, se crea la alarma (`PENDIENTE`) y se publica el evento `AlarmaCreada`.
3. Cuando la transacción termina bien (*commit*), `NotificadorAlarmas` manda un mail a todos los destinatarios.
4. Mientras la alarma esté pendiente, los datos siguientes sobre el umbral no crean otra alarma ni mandan otro mail.
5. Un `OPERADOR` o `ADMIN` la acepta: se guarda su usuario, fecha/hora y observación. Si después llega otro dato sobre el umbral, se vuelve a alarmar.

## Conceptos que aplica

- **Cálculo derivado en el backend.** El ETA y el tiempo no se guardan: se calculan en cada consulta con datos que ya existen, así nunca quedan desactualizados. `CalculosMonitoreo`.
- **Funciones puras y tests unitarios.** Reciben datos y devuelven un resultado, sin base ni reloj propio (el `ahora` entra por parámetro). `CalculosMonitoreoTest` prueba los bordes sin levantar Spring.
- **Filtros y orden en la base.** Una consulta JPQL con parámetros opcionales (`:estado is null or ...`) y un `Sort`. `OrdenRepository.buscarParaMonitoreo`.
- **Lista blanca para ORDER BY.** El cliente manda `ordenarPor=masa` y el servicio lo traduce a la propiedad real (`ultimaMasaAcumulada`). Si manda cualquier otra cosa, se usa el número de orden. Evita armar consultas con texto del cliente. `MonitoreoService.COLUMNAS_ORDENABLES`.
- **Polling.** El navegador pregunta cada 5 s. Es lo más simple para "se actualiza sola", y alcanza para estos tiempos. `usePolling`.
- **Eventos de Spring + `@TransactionalEventListener`.** `AlarmaService` avisa "se creó una alarma" y `NotificadorAlarmas` reacciona. El listener corre después del commit: nunca sale un mail de una alarma que no se guardó.
- **Tolerancia a fallas del mail.** Si el servidor SMTP falla, solo se escribe en el log. La alarma ya está guardada y el dato de carga se aceptó igual. Hay timeouts de 3 s para que no se cuelgue.
- **Control de concurrencia.** La orden se lee con `SELECT ... FOR UPDATE` (parte del parcial), así que dos datos simultáneos no pueden crear dos alarmas.
- **Configuración en la base.** Umbral y destinatarios viven en la tabla `configuracion_alarma` y el `ADMIN` los cambia desde la pantalla, sin reiniciar. Las variables de entorno solo dan el valor inicial (la primera vez que se pide la configuración). `AlarmaService.evaluar` la lee en cada dato, por eso el cambio rige enseguida.
- **Mailpit.** Es un servidor SMTP de prueba: recibe los mails y los muestra en una web (`http://localhost:8025`), sin mandar nada a Internet.
- **Autorización por rol.** Cualquiera logueado ve las alarmas, pero aceptarlas exige `OPERADOR` o `ADMIN` (`SecurityConfig`), y además el botón se oculta en el frontend (`auth.tieneRol`).

## Cómo probarlo

**Tests automáticos**

```powershell
cd backend
.\mvnw.cmd test     # 37 tests. Nuevos: CalculosMonitoreoTest, MonitoreoTest, AlarmasTest y ConfiguracionAlarmaTest (con el mail simulado)

cd ..\frontend
npm test            # 16 tests (incluye AlarmasView.test.js: el formulario de configuración)
npm run build
```

**A mano con Mailpit**

1. Levantar Mailpit: `docker run -d --name mailpit -p 8025:8025 -p 1025:1025 axllent/mailpit`
2. Backend: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"` y frontend: `npm run dev`.
3. Con los usuarios de demo: crear una orden (`sap`), registrar la tara (`tms`), y mandar datos de carga con masa creciente y temperatura mayor a 40 (`carga`). Un ejemplo:
   ```bash
   curl -X POST localhost:8080/api/v1/ordenes/9001/detalle -H "Authorization: Bearer <token carga>" \
        -H "Content-Type: application/json" \
        -d '{"masaAcumulada":2000,"densidad":0.5,"temperatura":45,"caudal":5000}'
   ```
4. Abrir `http://localhost:8025`: hay **un** mail. Seguir mandando datos (con masa mayor cada vez): no llegan más.
5. En `http://localhost:5173` entrar como `operador` / `demo1234`: **Monitoreo** muestra la orden con su ETA y el tiempo cargando; **Alarmas** muestra la pendiente. Como `admin` / `demo1234`, en **Alarmas** se ve el formulario de configuración: cambiar el umbral a 60 y el mail, guardar, y mandar un dato de 50 °C: ya no alarma. Con un dato de más de 60 llega el mail al destinatario nuevo. Como `operador` el formulario no aparece. Aceptarla con una observación y mandar otro dato sobre el umbral: llega un segundo mail.

## Preguntas posibles del coloquio

**¿Cómo se calcula el ETA y por qué solo en estado 2?**
`(preset − masa acumulada) / caudal`, en horas. Solo tiene sentido mientras se está cargando; una orden cerrada o finalizada ya no tiene "tiempo restante". Si el caudal es nulo o 0 devuelve `null` para no dividir por cero.

**¿Por qué el ETA no se guarda en la base?**
Cambia con cada dato nuevo y se deduce de valores que ya guardamos. Calcularlo al consultar evita que quede desactualizado y no ocupa columnas.

**¿Por qué polling y no WebSockets?**
El enunciado pide que se actualice solo, no instantáneamente. Con polling el servidor sigue siendo una API REST común, y el código es corto y fácil de explicar. WebSockets o SSE serían mejores con muchísimos clientes o necesidad de latencia mínima; acá sumarían complejidad sin beneficio.

**¿Dónde se hacen los filtros y el orden, y por qué ahí?**
En el backend con una consulta a la base. Así el frontend no baja datos que no muestra, y el orden por columnas está controlado con una lista blanca, no con texto libre del cliente (que podría ser un intento de inyección).

**¿Cómo se evita mandar un mail por cada dato que supera el umbral?**
Antes de crear una alarma se pregunta si la orden ya tiene una `PENDIENTE` (`existsByOrdenAndEstado`). Si la tiene, no se hace nada. Solo después de aceptarla se puede volver a alarmar.

**¿Qué pasa si llegan dos datos al mismo tiempo sobre el umbral?**
`CargaService` lee la orden con `SELECT ... FOR UPDATE`, así que se procesan de a uno: el primero crea la alarma y el segundo ya la encuentra pendiente.

**¿Por qué el mail se manda después del commit y no dentro de la transacción?**
Si se mandara antes y la transacción fallara, saldría un mail de una alarma que no existe. Con `@TransactionalEventListener` el mail solo sale si todo se guardó.

**¿Qué pasa si el servidor de mail está caído?**
Se registra el error en el log y listo: la alarma está guardada y se ve en la pantalla, y el dato de carga se recibió normalmente. Tampoco se cuelga: hay timeouts de 3 segundos. Es un test (`siFallaElMailLaAlarmaIgualQuedaGuardada...`).

**¿Quién puede aceptar una alarma y cómo se registra?**
`OPERADOR` y `ADMIN`. El usuario sale del token (`UsuarioActual.nombre()`), no del body, así nadie puede aceptar "a nombre de otro". La fecha/hora la pone el servidor.

**¿Cómo se cambia el umbral o los destinatarios?**
Un `ADMIN` entra a **Alarmas**, edita el formulario de configuración y guarda (por debajo es `PUT /api/v1/alarmas/configuracion`). Se guarda en la base y rige desde el próximo dato de carga, sin reiniciar. Las variables `ALARMA_TEMPERATURA_UMBRAL` y `ALARMA_DESTINATARIOS` solo se usan como valor inicial, cuando la tabla todavía está vacía (decisión 24 en `docs/decisiones.md`).

**¿Por qué solo el `ADMIN` puede cambiarla?**
Un umbral mal puesto apaga las alarmas o llena de mails a alguien, así que es una acción de administración. Se exige en el backend (`SecurityConfig`, devuelve 403 a los demás) y además el formulario se oculta en el frontend, que es solo comodidad.

**¿Para qué sirve Mailpit?**
Para probar los mails sin un servidor real ni mandar nada a Internet: recibe por SMTP y los muestra en una web. En el Docker Compose se levanta junto al resto.
