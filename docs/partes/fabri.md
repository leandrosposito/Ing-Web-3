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
| `service/NotificadorAlarmas.java` | Escucha el evento y manda el mail (después del commit, en otro hilo con `@Async`). |
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
3. Cuando la transacción termina bien (*commit*), `NotificadorAlarmas` manda un mail a todos los destinatarios, en otro hilo (`@Async`) para que un SMTP lento no demore la respuesta al sistema de carga.
4. Mientras la alarma esté pendiente, los datos siguientes sobre el umbral no crean otra alarma ni mandan otro mail.
5. Un `OPERADOR` o `ADMIN` la acepta: se guarda su usuario, fecha/hora y observación. Si después llega otro dato sobre el umbral, se vuelve a alarmar.

## El problema N+1 (monitoreo y listado de órdenes)

**Qué es.** Una `Orden` tiene un camión, un chofer, un cliente y un producto. Si pedimos las órdenes con una consulta simple, Hibernate trae las órdenes (**1** consulta) y después, para cada orden, hace otra consulta por cada entidad asociada que todavía no tiene cargada (**N** órdenes × 4 entidades). Con 100 órdenes serían 1 + hasta 400 consultas para mostrar una tabla, y crece con cada orden nueva. Se llama "N+1" porque son las N consultas extra que se suman a la primera. No da error y con pocos datos ni se nota, por eso es fácil que pase desapercibido.

**Cómo se resolvió.** Con `join fetch` en la consulta JPQL: le pedimos a la base que traiga la orden *junto con* su camión, chofer, cliente y producto en una sola consulta con `JOIN`.

```java
@Query("""
        select o from Orden o
        join fetch o.camion c join fetch o.chofer join fetch o.cliente join fetch o.producto
        where (:estado is null or o.estado = :estado)
          and lower(c.patente) like lower(concat('%', :patente, '%'))""")
List<Orden> buscarParaMonitoreo(EstadoOrden estado, String patente, Sort sort);
```

Se aplicó a `buscarParaMonitoreo` (monitoreo) y a las dos consultas del listado de órdenes (`findAllByOrderByNumeroOrdenAsc` y `findByEstadoOrderByNumeroOrdenAsc`). El listado además muestra la capacidad total del camión, que sale de sus cisternas (una colección); como no conviene meterla en el mismo `join fetch` (multiplicaría las filas), se le puso `@BatchSize(size = 50)` a `Camion.cisternas`: las cisternas de muchos camiones se traen en **una** consulta en lugar de una por camión.

**Cómo lo comprobamos.** `ConsultasSinN1Test` activa las estadísticas de Hibernate, crea 4 órdenes y cuenta las consultas SQL: el monitoreo hace **1** y el listado **2 como máximo**, sin importar cuántas órdenes haya. Probado sin el `join fetch`, el mismo test falla: el monitoreo hacía 12 consultas y el listado 9 para solo 4 órdenes.

---

## Cómo probarlo

**Tests automáticos**

```powershell
cd backend
.\mvnw.cmd test     # 48 tests en total. Los de Fabri: CalculosMonitoreoTest, MonitoreoTest, AlarmasTest, ConfiguracionAlarmaTest (con el mail simulado) y ConsultasSinN1Test

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
