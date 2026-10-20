# Decisiones de diseño e interpretación del enunciado

Este documento junta lo que decidimos y por qué. Sirve para preparar el coloquio: cualquiera del grupo tiene que poder defenderlo.

## 1. Interpretaciones de partes ambiguas del enunciado

| # | Ambigüedad | Qué hicimos | Por qué |
|---|---|---|---|
| 1 | ¿Quién genera la contraseña de activación? La tabla de estados dice "desde el sistema TMS", pero el diagrama la ubica en el backend. | La genera el **backend** (entero aleatorio de 10000 a 99999, con `SecureRandom`) y la devuelve en la respuesta del pesaje inicial. | El backend es el que después la valida en la habilitación, así que tiene que conocerla. Coincide con el diagrama ("Se genera la password de activación"). |
| 2 | ¿En qué estado se carga? El anexo dice "estado 2: en proceso de carga", pero el diagrama de secuencia pone la carga junto al estado 3. | Los datos de carga solo se aceptan en **estado 2**. El cierre pasa la orden a 3 y desde ahí se rechazan (409). | El punto 4 dice que el cierre "bloquea la recepción de nuevos datos de detalle", así que la carga tiene que ocurrir antes, en el estado 2. |
| 3 | Número de orden: el punto 1 dice que los códigos externos son `string`, y el anexo dice que el número de orden es un "identificador único (entero)". | `numeroOrden` es un `Long` único y funciona como código externo de la orden. Camión, chofer, cliente y producto usan `codigoExterno` de tipo `String`. | Respeta las dos partes del enunciado. El punto 3 llama al número de orden "código externo". |
| 4 | Densidad fuera de rango. El enunciado dice que vale entre 0 y 1, pero no la incluye en los criterios de descarte. | Se **descarta** el dato si la densidad no está en (0, 1]. | Un dato fuera del rango definido no es un "valor válido", y la cabecera solo guarda valores válidos. Si la cátedra lo interpreta distinto, se cambia en una sola línea de `ReglasDetalleCarga`. |
| 5 | Masa igual a la anterior. | Se **acepta**. | El enunciado descarta solo la masa "menor que el valor anterior". Una masa igual puede pasar si la bomba no avanzó entre dos lecturas. |
| 6 | ¿Cómo se responde un dato descartado? | `200 OK` con `aceptado: false` y `motivoDescarte`. | El request está bien formado; el dato simplemente no cumple las reglas. El sistema de carga manda datos todo el tiempo y no debería tratar esto como un error. Si faltan campos, sí es un error del request: 400. |
| 7 | ¿Se valida la contraseña en cada dato de carga? | No, solo en la habilitación (3.1). Cada dato se valida contra el estado de la orden. | En el diagrama, el paso 3.3 solo envía los datos de carga. |
| 8 | ¿Pesaje final menor que la tara? | No se rechaza; solo se exige que sea positivo. | El enunciado no lo pide. Una diferencia rara queda visible en la conciliación, que es justamente para eso. |
| 9 | "Usuario o proceso" en la auditoría. | Se registra el sistema que hizo el cambio: `SAP`, `TMS` o `SISTEMA_CARGA`. | En el parcial no hay login. En el final, con seguridad, pasa a ser el usuario autenticado. |
| 10 | ¿Qué pasa si SAP manda un camión (o chofer, cliente, producto) que ya existe? | Upsert por `codigoExterno`: si existe se actualizan sus datos (incluido el cisternado), si no se crea. | "Sincronizar" implica mantener nuestros datos iguales a los de SAP. Ojo: como es la misma entidad, el cambio se ve en todas las órdenes que la usan. |
| 11 | ¿Se puede cerrar una orden sin datos de carga? | Sí. En ese caso, el producto cargado de la conciliación es 0 y los promedios son `null`. | El enunciado no lo prohíbe, y una carga puede abortarse. |
| 12 | Inicio y fin de carga. | Inicio = momento del primer dato **válido** recibido. Fin = momento del último dato válido recibido. | Es lo que dice el anexo. Usamos los datos recibidos, no solo los almacenados. |

## 2. Decisiones de diseño

- **Endpoints por evento, no PATCH del estado.** Cada transición es un `POST` a un sub-recurso (`/pesaje-inicial`, `/cierre`, etc.). Son eventos con efectos secundarios (generar la contraseña, calcular la conciliación), y así nadie puede saltearse las reglas mandando `{ "estado": 4 }`.
- **`numeroOrden` en la URL, no el `id` interno.** Los sistemas externos solo conocen el número de orden. Los IDs internos nunca salen de la API.
- **Un único lugar donde cambia el estado.** `OrdenService.cambiarEstado()` valida la transición con `EstadoOrden.puedePasarA()` (solo al estado siguiente) y guarda el registro de auditoría. Todo ocurre dentro de la transacción del método que lo llama: si falla la auditoría, el estado no cambia.
- **Concurrencia.** Las operaciones que modifican una orden la leen con `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`). Si llegan dos datos de carga a la vez, o un dato y el cierre, se procesan de a uno. Así no se pisa la "última masa" ni entra un dato después del cierre.
- **La conciliación no se persiste.** Se calcula a partir de datos que ya están en la base (los promedios con `AVG` en la query), así nunca queda desactualizada.
- **Frecuencia de almacenamiento.** La cabecera se actualiza con cada dato válido. Un registro de detalle se guarda si pasaron al menos N segundos desde el último guardado (N configurable; 0 = guardar todos). La regla es una función pura (`ReglasAlmacenamiento`), testeada con horas fijas.
- **DTOs como `record` y mapper manual.** Queda explícito qué se expone (por ejemplo, la contraseña solo sale en la respuesta del pesaje inicial). Para pocas clases no justifica sumar una librería de mapeo.
- **Errores.** Un único `@RestControllerAdvice` traduce las excepciones a 400/401/404/409 con el mismo formato JSON.
- **Perfiles.** MySQL por defecto (las credenciales salen de variables de entorno o de un `.env` que no se sube). El perfil `h2` y los tests usan H2 en memoria, así se puede probar sin instalar nada.

## 3. Conceptos aplicados (para el coloquio)

| Concepto | Dónde está en el proyecto |
|---|---|
| **API REST** | Recurso `/ordenes/{numeroOrden}`, verbos HTTP con su significado y códigos de estado semánticos (201, 400, 401, 404, 409). |
| **Arquitectura en capas** | `controller` → `service` → `repository`. Los controllers no tienen lógica; las reglas están en los services y se testean sin HTTP. |
| **DTOs** | Paquete `dto`: lo que manda SAP no es igual a la entidad `Orden`, y la conciliación es un DTO calculado. |
| **Máquina de estados** | `EstadoOrden` + `OrdenService.cambiarEstado()`. Test: `EstadoOrdenTest`. |
| **Validaciones** | De formato con Bean Validation en los DTOs (`@NotNull`, `@Positive`, `@Valid`) → 400. De negocio en los services (estado, contraseña, reglas de descarte). |
| **Manejo centralizado de excepciones** | `GlobalExceptionHandler` + excepciones propias en `exception`. |
| **ORM / JPA** | Entidades en `model`. Relaciones `@ManyToOne` (orden → maestros) y `@OneToMany` (camión → cisternas con `cascade` y `orphanRemoval`). Hibernate genera las tablas (`ddl-auto=update`). |
| **Transacciones** | `@Transactional` en los services: alta + sincronización + auditoría en una sola transacción; `readOnly` en las consultas. |
| **Locking pesimista** | `OrdenRepository.findByNumeroOrdenParaActualizar()`. |
| **Integración por código externo** | `SincronizacionService` (upsert por `codigoExterno`). |
| **Auditoría** | Entidad `CambioEstado` + endpoint `/historial-estados`. |
| **OpenAPI / Swagger** | springdoc genera el spec a partir de los controllers, más `@Operation` / `@ApiResponse` con los errores reales de cada endpoint. |
| **Configuración externalizada** | `application.properties` con `${VARIABLE:default}`, `.env` ignorado por git, perfil `h2`. |
| **Testing** | Unitarios (JUnit) para las reglas puras, integración con `MockMvc` para el circuito completo, y la colección Postman con tests en JS. |

## 4. Seguridad y login (parte de Fran)

| # | Decisión | Por qué |
|---|---|---|
| 13 | **Un solo rol por usuario** (`Usuario.rol`, enum con SAP, TMS, SISTEMA_CARGA, OPERADOR, ADMIN). | Cada integración hace una sola cosa. Varios roles por usuario obligarían a una tabla intermedia sin ningún beneficio acá. |
| 14 | **JWT con la librería jjwt**, firmado con HS256 y con vencimiento configurable (120 min por defecto). | Es la opción más directa y fácil de explicar: generar y validar un token son 2 métodos. No hay refresh token: al vencer, se vuelve a loguear. |
| 15 | **Los `GET` los puede usar cualquier usuario logueado.** | El enunciado dice que la conciliación la consulta "cualquier cliente", y las consultas no modifican nada. Los `POST` sí se restringen por rol (ADMIN puede todo). |
| 16 | **La auditoría guarda el username** (`sap`, `tms`, `carga`...), ya no el nombre del sistema. | Cierra el punto 9 de la sección 1: en el final el "usuario o proceso" es el usuario autenticado. |
| 17 | **Usuarios de demo creados al arrancar** con la contraseña de `PASSWORD_DEMO` (por defecto `demo1234`, solo desarrollo). | Los equipos necesitan poder entrar sin cargar datos a mano, y la contraseña no queda fija en el código. No hay ABM de usuarios porque el enunciado no lo pide. |
| 18 | **El token se guarda en `localStorage`** y se manda en el header `Authorization`. | Es lo más simple de implementar y explicar. El riesgo (XSS) se mitiga con tokens de vida corta. |
| 19 | **Los 401/403 los arma `SecurityConfig` con el mismo JSON** que `GlobalExceptionHandler`. | Esos errores ocurren en los filtros, antes de llegar a un controller, por eso el `@RestControllerAdvice` no los ve. |
| 39 | **La consola de H2 (`/h2-console`) solo existe con el perfil `h2`**: la habilita `application-h2.properties` y su seguridad (`H2ConsoleSecurityConfig`, `@Profile("h2")`) la deja pasar sin token, sin CSRF y con `frameOptions` en `sameOrigin`. Sin el perfil, queda cerrada (401 sin token, 404 con token). | Antes el perfil `h2` la habilitaba pero la cadena de seguridad la bloqueaba (pedía JWT y prohibía los frames), así que no se podía usar. Abrirla sin autenticación es aceptable solo para la base en memoria de desarrollo; en un servidor sería acceso directo a la base. La API sigue protegida igual. |

## 5. Monitoreo y alarmas (parte de Fabri)

| # | Decisión | Por qué |
|---|---|---|
| 20 | **ETA y tiempo transcurrido solo para órdenes en estado 2.** Sin caudal válido (nulo o ≤ 0), el ETA es `null`. Si la masa ya alcanzó el preset, el ETA es 0. | El enunciado pide el tiempo "solo para estado 2" y el ETA es el tiempo que falta para llenar: no tiene sentido en una orden cerrada o sin cargar. `null` evita dividir por cero. |
| 21 | **Los valores derivados se calculan al consultar**, no se guardan. | Se deducen de datos que ya están en la cabecera de la orden, así nunca quedan desactualizados. |
| 22 | **Filtros (estado, patente) y orden se resuelven en el backend**, con una lista blanca de columnas. | El frontend no baja lo que no muestra y no se arma un `ORDER BY` con texto del cliente. |
| 23 | **Actualización automática por polling cada 5 s** (no WebSockets ni SSE). | Alcanza para "se actualiza sola", mantiene la API REST común y es lo más fácil de explicar. |
| 24 | **Umbral y destinatarios se guardan en la base y se editan desde el sistema** (tabla `configuracion_alarma`, una sola fila). `GET` y `PUT /api/v1/alarmas/configuracion`, solo `ADMIN`; formulario en la pantalla de alarmas, visible solo para `ADMIN`. Las variables `ALARMA_TEMPERATURA_UMBRAL` y `ALARMA_DESTINATARIOS` quedan como **valor inicial**: se usan una sola vez, cuando la tabla está vacía. Después manda la base (si se cambian las variables con la tabla ya creada, no pasa nada). El remitente sigue siendo una variable. | Primero se había decidido por variables de entorno (el enunciado decía "configurables"), pero obligaba a reiniciar el backend; se cambió por pedido del equipo. El cambio rige en el próximo dato porque se lee la configuración en cada evaluación. Validación: umbral entre -50 y 200, de 1 a 10 mails válidos. Se guardan quién y cuándo cambió la configuración. La alarma guarda el umbral con el que se disparó, y el evento lleva los destinatarios vigentes en ese momento. |
| 25 | **Alarma = dato válido con `temperatura > umbral`**. Un dato descartado no alarma. El umbral usado queda guardado en la alarma. | La cabecera solo guarda datos válidos. Guardar el umbral permite auditar la alarma aunque después cambie la configuración. |
| 26 | **Una sola alarma pendiente por orden.** Mientras exista, no se crean más ni se mandan más mails; después de aceptarla, un nuevo dato sobre el umbral vuelve a alarmar. | Es la regla "no se vuelve a enviar hasta que un usuario autorizado la acepte". |
| 27 | **El mail se manda después del commit** (`@TransactionalEventListener`) y **si falla solo se registra en el log**. | No debe salir un mail de una alarma que no se guardó, y un SMTP caído no debe impedir recibir los datos de carga. La alarma queda visible en la pantalla igualmente. |
| 28 | **Aceptar una alarma: solo `OPERADOR` y `ADMIN`.** La observación es opcional; el usuario sale del token y la fecha/hora del servidor. | "Usuario autorizado" del enunciado. Tomar el usuario del token evita aceptar a nombre de otro. |
| 37 | **Las consultas de monitoreo y del listado de órdenes usan `join fetch`** de camión, chofer, cliente y producto, y las cisternas se traen con `@BatchSize(50)`. | Evita el problema N+1: sin el `join fetch`, 4 órdenes hacían 12 consultas en el monitoreo y 9 en el listado, y crecía con cada orden. Ahora son 1 y como máximo 2, sin importar cuántas órdenes haya (`ConsultasSinN1Test`). |
| 38 | **El mail de la alarma se manda con `@Async`** (más `@EnableAsync`), después del commit. | El SMTP puede ser lento y el sistema de carga manda datos todo el tiempo: no debe esperar al mail para recibir su respuesta. Complementa el punto 27 (si el mail falla solo se registra en el log). |

## 6. Conciliaciones, despliegue y Postman (parte de Lean)

| # | Decisión | Por qué |
|---|---|---|
| 29 | **La pantalla de conciliaciones usa los endpoints del parcial** (`GET /ordenes?estado=FINALIZADA` y `GET /ordenes/{n}/conciliacion`); no se agregó nada al backend. | Ya hacían exactamente lo que necesita la pantalla. Menos piezas, menos para explicar. |
| 30 | **Sin refresco automático en conciliaciones** (solo un botón "Actualizar lista"). | Una orden finalizada no cambia, a diferencia del monitoreo. |
| 31 | **Docker Compose con 4 servicios** (MySQL, backend, frontend con nginx, Mailpit) y **Dockerfiles de dos etapas**. | Un solo comando levanta todo; la imagen final no lleva las herramientas de compilación. |
| 32 | **nginx del frontend reenvía `/api` al backend**, y el frontend llama siempre a rutas relativas. | Igual que el proxy de Vite en desarrollo. El navegador habla con un único origen y no hace falta configurar CORS. |
| 33 | **Solo se publican los puertos 3000 (web), 8080 (API/Swagger) y 8025 (Mailpit).** MySQL queda interno. | Evita choques con un MySQL local en el 3306 y no expone la base. |
| 34 | **Valores por defecto de desarrollo en el compose** (`${VAR:-valor}`); para cambiarlos se usa un `.env` que no se sube (hay un `.env.example`). | El proyecto arranca sin configurar nada (útil para el coloquio), sin dejar secretos reales en el repo. |
| 35 | **Postman: un login por usuario de demo y cada request con el usuario que le corresponde.** Se guarda un token por usuario en variables de colección (`tokenSap`, `tokenTms`, `tokenCarga`, `tokenOperador`, `tokenAdmin`): `sap` crea órdenes, `tms` hace los pesajes, `carga` habilita, envía datos y cierra, `operador` consulta y acepta alarmas, y `admin` solo cambia la configuración de alarmas. Incluye un 403 de ejemplo (`tms` intentando crear una orden), el flujo de alarmas (dato sobre el umbral → alarma pendiente → aceptar con `operador` → aceptar de nuevo da 409) y la verificación de que el historial de estados muestra `sap`, `tms`, `carga`, `tms`. | Con un solo usuario `admin` la colección no probaba la seguridad: nada fallaba aunque los permisos estuvieran mal. Así cada request prueba que su rol alcanza, el 403 prueba que no alcanza otro, y el historial prueba que la auditoría guarda el usuario autenticado. El dato de alarma usa 200 °C para disparar cualquier umbral razonable sin depender de la configuración vigente. |
| 36 | **La regla "guardar o no este dato" es una función pura** (`ReglasAlmacenamiento.debeAlmacenar(ultimoGuardado, ahora, frecuencia)`), separada de `CargaService`. | Se puede testear con horas fijas, sin base ni reloj: con frecuencia de 10 s y un dato por segundo durante 60 s se guardan 6, como en el ejemplo del enunciado (`ReglasAlmacenamientoTest`). `CargaService` solo busca la fecha del último detalle guardado y le pregunta a la regla. |
