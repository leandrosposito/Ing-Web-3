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
- **Frecuencia de almacenamiento.** La cabecera se actualiza con cada dato válido. Un registro de detalle se guarda si pasaron al menos N segundos desde el último guardado (N configurable; 0 = guardar todos).
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
