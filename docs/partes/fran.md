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
| `security/H2ConsoleSecurityConfig.java` | Cadena de seguridad propia para `/h2-console`, **solo con el perfil `h2`**: sin token (la consola tiene su propio login de base), sin CSRF y con `frameOptions` en `sameOrigin` porque la consola se dibuja con frames. Sin el perfil la clase no existe y la consola no está habilitada. |
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
| `src/api/errores.js` | Convierte la respuesta de error del backend en un `Error` con su mensaje. |
| `src/router/index.js` | Rutas y *guard*: sin sesión manda a `/login`. Cada ruta nueva va en una línea. |
| `src/views/LoginView.vue` | Formulario de login. |
| `src/App.vue` | Barra superior con el usuario y el botón Salir. |
| `src/stores/auth.test.js` | 7 tests (store, `api()` y guard). |

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
