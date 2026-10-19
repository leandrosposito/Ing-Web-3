# Parte de Fran: seguridad y login

Rama `fran`. Agrega autenticación con JWT, roles por endpoint, auditoría con el usuario real y el frontend con login.

## Qué se implementó

### Backend (`backend/src/main/java/ar/edu/iua/iw3/`)

| Archivo | Qué hace |
|---|---|
| `model/Usuario.java`, `model/Rol.java`, `repository/UsuarioRepository.java` | Usuario (username, hash BCrypt, rol) y los 5 roles. |
| `controller/AuthController.java` | `POST /api/v1/auth/login`: recibe usuario y contraseña, devuelve el token. |
| `security/AuthService.java` | Verifica la contraseña con BCrypt y pide el token. |
| `security/JwtService.java` | Genera el JWT (HS256, con usuario y rol) y lo valida. |
| `security/JwtAuthFilter.java` | En cada request lee `Authorization: Bearer ...` y deja al usuario autenticado. |
| `security/SecurityConfig.java` | Qué rol puede llamar a cada endpoint, API sin sesión, respuestas 401/403 en JSON. |
| `security/UsuariosDemo.java` | Crea un usuario por rol al arrancar si la tabla está vacía. |
| `security/UsuarioActual.java` | Devuelve el nombre del usuario autenticado. |
| `service/OrdenService.java` | `cambiarEstado()` guarda en la auditoría al usuario autenticado. |
| `exception/GlobalExceptionHandler.java` | Login incorrecto → 401. |
| `config/OpenApiConfig.java` | Botón **Authorize** en Swagger. |

### Quién puede qué

| Rol | Endpoints |
|---|---|
| `SAP` | `POST /ordenes` |
| `TMS` | `POST /ordenes/{n}/pesaje-inicial` y `/pesaje-final` |
| `SISTEMA_CARGA` | `POST /ordenes/{n}/habilitar-carga`, `/detalle` y `/cierre` |
| `OPERADOR` | consultas (y, más adelante, aceptar alarmas) |
| `ADMIN` | todo |
| cualquier usuario logueado | todos los `GET` (consultas y conciliación) |
| sin token | solo `login` y Swagger |

### Usuarios de demo

`sap`, `tms`, `carga`, `operador` y `admin`. Todos con la contraseña de la variable `PASSWORD_DEMO` (por defecto `demo1234`, solo para desarrollo).

### Frontend (`frontend/`)

| Archivo | Qué hace |
|---|---|
| `src/stores/auth.js` | Guarda token, usuario y rol (Pinia + `localStorage`). Acciones `login` y `logout`. |
| `src/api/http.js` | Función `api()` que agrega el header `Authorization`. Si el backend responde 401, cierra la sesión. |
| `src/api/errores.js` | Convierte las respuestas de error del backend en un `ApiError` con su mensaje. |
| `src/router/index.js` | Rutas y *guard*: sin sesión manda a `/login`. Cada ruta nueva va en una línea. |
| `src/views/LoginView.vue` | Formulario de login. |
| `src/App.vue` | Barra superior con el usuario y el botón Salir. |
| `src/stores/auth.test.js` | 7 tests (store, `api()` y guard). |

## Conceptos que aplica

- **Autenticación vs. autorización.** Autenticar es saber quién sos (login); autorizar es saber qué podés hacer (rol). Login: `AuthService`. Roles: `SecurityConfig`.
- **JWT.** Un token firmado que lleva usuario y rol. El servidor no guarda sesiones (*stateless*): solo comprueba la firma. En `JwtService`. Se manda en el header `Authorization: Bearer <token>`.
- **BCrypt.** Las contraseñas se guardan como hash con sal, nunca en texto plano. `UsuariosDemo` y `AuthService` (`passwordEncoder.matches`).
- **Cadena de filtros de Spring Security.** Antes de llegar al controller, el request pasa por filtros. `JwtAuthFilter` es el nuestro y se agrega con `addFilterBefore` en `SecurityConfig`.
- **RBAC (control de acceso por roles).** Cada endpoint exige ciertos roles con `hasAnyRole(...)`. Las reglas se leen de arriba hacia abajo y gana la primera que coincide.
- **401 vs. 403.** 401: no sabemos quién sos (sin token, token malo o vencido). 403: sabemos quién sos pero tu rol no alcanza. Ambos en `SecurityConfig` con el mismo JSON de error del resto de la API.
- **CSRF deshabilitado.** CSRF ataca sesiones con cookies; acá el token va en un header que el navegador no manda solo. `SecurityConfig`.
- **Auditoría con usuario real.** `UsuarioActual.nombre()` lee el `SecurityContext` y `OrdenService.cambiarEstado()` lo guarda en el log de estados.
- **Configuración externalizada.** Secreto del JWT, expiración y contraseña de demo salen de variables de entorno (`application.properties`, `.env.example`).
- **Guard de router y estado global (Vue).** `beforeEach` bloquea las rutas privadas; el store de Pinia guarda la sesión y `App.vue` redirige al login cuando se cierra.
- **Proxy de desarrollo.** `vite.config.js` reenvía `/api` al backend, así no hay CORS.

## Cómo probarlo

**Tests automáticos**

```powershell
cd backend
.\mvnw.cmd test        # 19 tests: SeguridadTest (login, 401, 403 por rol) y CircuitoOrdenTest (circuito con un usuario por rol)

cd ..\frontend
npm install
npm test               # 7 tests
npm run build
```

**A mano** (con `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"` en `backend/` y `npm run dev` en `frontend/`):

```bash
# sin token -> 401
curl -i localhost:8080/api/v1/ordenes

# login -> devuelve el token
curl -X POST localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
     -d '{"username":"tms","password":"demo1234"}'

# con TMS: consultar funciona (200), crear una orden no (403)
curl localhost:8080/api/v1/ordenes -H "Authorization: Bearer <token>"
curl -X POST localhost:8080/api/v1/ordenes -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" -d '{}'
```

En el navegador: `http://localhost:5173` redirige a `/login`; entrá con `operador` / `demo1234` y se ve el inicio con tu usuario y rol. En Swagger (`/swagger-ui.html`) pegá el token en **Authorize**.

## Preguntas posibles del coloquio

**¿Qué es un JWT y por qué no hace falta guardar sesiones?**
Es un texto con tres partes (cabecera, datos y firma). Los datos (usuario, rol, vencimiento) van firmados con una clave que solo conoce el servidor. En cada request el servidor recalcula la firma: si coincide y no venció, confía en lo que dice. No guarda nada por usuario.

**¿Cualquiera puede leer el contenido de un JWT?**
Sí, está codificado en Base64, no cifrado. Por eso no lleva contraseñas, solo usuario y rol. Lo que lo protege es la firma: si alguien cambia el rol, la firma deja de coincidir (`tokenAdulteradoDa401` lo prueba).

**¿Por qué BCrypt y no guardar la contraseña o usar SHA-256?**
Si se filtra la base, un hash BCrypt no se puede revertir. Además usa una sal distinta por contraseña y es lento a propósito, así que probar millones de contraseñas es caro. SHA-256 es muy rápido, lo que facilita el ataque por fuerza bruta.

**¿Diferencia entre 401 y 403?**
401: no estás autenticado (sin token, mal firmado o vencido). 403: estás autenticado pero tu rol no permite ese endpoint.

**¿Por qué el login dice "usuario o contraseña incorrectos" sin aclarar cuál falló?**
Para no revelar qué usuarios existen. `AuthService` da el mismo error en los dos casos.

**¿Por qué desactivaron CSRF?**
CSRF se aprovecha de que el navegador manda cookies solo. Acá no hay cookies ni sesión: el token lo agrega el código del frontend en un header, así que un sitio ajeno no puede usarlo.

**¿Dónde guarda el token el frontend y qué riesgo tiene?**
En `localStorage`. Es simple, pero un ataque XSS podría leerlo. Se mitiga con tokens de vida corta (120 min configurables) y porque Vue escapa por defecto lo que muestra. La alternativa más segura es una cookie `HttpOnly`, que complica CSRF y CORS.

**¿Cómo se registra el usuario en la auditoría?**
El filtro deja al usuario en el `SecurityContext`. `OrdenService.cambiarEstado()` llama a `UsuarioActual.nombre()` y lo guarda junto al cambio de estado. Es el único lugar donde cambia el estado, así que ningún cambio queda sin usuario.

**¿Qué pasa si el token vence mientras uso el frontend?**
El backend responde 401, `api()` cierra la sesión y `App.vue` redirige al login.

**¿Por qué un solo rol por usuario?**
Para este sistema alcanza: cada integración externa hace una sola cosa. Con varios roles por usuario habría que agregar una tabla intermedia; se decidió no hacerlo (ver `docs/decisiones.md`).

**¿Se puede cambiar quién accede a un endpoint?**
Sí, en una sola línea de `SecurityConfig`. Los tests de `SeguridadTest` fallarían si cambia un permiso sin querer.
