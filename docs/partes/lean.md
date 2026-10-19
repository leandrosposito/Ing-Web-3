# Parte de Lean: conciliaciones, despliegue y documentación

Rama `lean`. Cuatro cosas, todas lo más simples posible:

1. La **pantalla de conciliaciones**.
2. **Docker Compose**: levantar todo el sistema con un solo comando.
3. La **colección de Postman** con login.
4. El **README** con cómo levantar el proyecto y la checklist al día.

---

## 1. Pantalla de conciliaciones

### Qué hace
Una pantalla con dos paneles. A la izquierda, la lista de órdenes **finalizadas** (estado 4). Al hacer click en una, a la derecha aparece su conciliación: tara, pesaje final, producto cargado, neto por balanza, diferencia y los promedios.

### Archivos
| Archivo | Qué tiene |
|---|---|
| `frontend/src/views/ConciliacionesView.vue` | La pantalla completa (lista + detalle). |
| `frontend/src/views/ConciliacionesView.test.js` | 2 tests de la pantalla. |
| `frontend/src/router/index.js` y `App.vue` | La ruta `/conciliaciones` y el link del menú. |

### Cómo funciona (son solo dos llamadas a la API)
1. Al abrirse la pantalla: `GET /api/v1/ordenes?estado=FINALIZADA` → la lista de la izquierda.
2. Al hacer click en una fila: `GET /api/v1/ordenes/{numero}/conciliacion` → la tabla de la derecha.

**No hizo falta tocar el backend:** los dos endpoints ya existían del parcial. La conciliación **no se guarda en la base**: el backend la calcula cada vez a partir de los datos de la orden, así nunca queda desactualizada.

Los cálculos (para saber explicarlos):

| Dato | Cuenta |
|---|---|
| Neto por balanza | pesaje final − pesaje inicial |
| Diferencia | neto por balanza − producto cargado |
| Producto cargado | última masa acumulada que midió el caudalímetro |
| Promedios | temperatura, densidad y caudal de los datos almacenados |

**Por qué es simple:** una sola pantalla, sin librerías de tablas (un `v-for` alcanza), sin refresco automático (las órdenes finalizadas no cambian; hay un botón "Actualizar lista"), y la lista ya viene filtrada desde el backend.

---

## 2. Docker Compose (explicado desde cero)

### El problema que resuelve
Para correr este sistema "a mano" hay que instalar Java, Node, MySQL, un servidor de mails de prueba, configurar variables y abrir 3 terminales. Si a un compañero (o al profe) le falta algo, no funciona. **Docker permite empaquetar cada pieza con todo lo que necesita**, para que ande igual en cualquier computadora.

### Los 4 conceptos que hay que entender

**Imagen.** Es como una "receta ya cocinada": un paquete con un programa y todo lo que necesita para correr (por ejemplo, MySQL ya instalado). Se descarga o se arma una vez. *Ejemplo: la imagen `mysql:8.4` trae MySQL 8.4 listo.*

**Contenedor.** Es una imagen **en funcionamiento**: un mini-computador aislado corriendo ese programa. De una misma imagen se pueden crear varios contenedores. *Analogía: la imagen es la clase y el contenedor es el objeto; o la imagen es el instalador y el contenedor es el programa abierto.*

**Dockerfile.** Es el archivo con las instrucciones para **armar una imagen propia** (la de nuestro backend y la de nuestro frontend). Cada línea es un paso: "partí de esta imagen base, copiá estos archivos, corré este comando".

**Docker Compose.** Es un archivo (`docker-compose.yml`) que describe **varios contenedores juntos**: cuáles son, cómo se configuran y cómo se conectan. Con un solo comando (`docker compose up`) los levanta a todos.

### Qué hay en nuestro proyecto

```
docker-compose.yml        <- describe los 4 servicios
backend/Dockerfile        <- receta de la imagen del backend
frontend/Dockerfile       <- receta de la imagen del frontend
frontend/nginx.conf       <- configuración del servidor web del frontend
.env.example              <- variables que se pueden cambiar
```

Los 4 servicios del `docker-compose.yml`:

| Servicio | Qué es | Imagen | Puerto en tu PC |
|---|---|---|---|
| `mysql` | La base de datos | `mysql:8.4` (oficial) | — (solo lo usa el backend) |
| `mailpit` | Servidor de mails **de prueba** (recibe las alarmas y las muestra en una web, no manda nada a Internet) | `axllent/mailpit` (oficial) | 8025 |
| `backend` | Nuestra API Spring Boot | se arma con `backend/Dockerfile` | 8080 |
| `frontend` | Nuestras pantallas Vue | se arma con `frontend/Dockerfile` | 3000 |

### Cómo se conectan entre sí
Compose crea una **red privada** donde cada servicio se encuentra por su **nombre**. Por eso el backend usa `mysql` como dirección de la base (`jdbc:mysql://mysql:3306/...`) y `mailpit` como servidor de mails. No hay que averiguar IPs.

```
Navegador ──> frontend (nginx, puerto 3000) ──/api──> backend (8080) ──> mysql
                                                          └──────────> mailpit (mails)
```

### Los Dockerfiles: "construcción en dos etapas"
Los dos Dockerfiles tienen **dos etapas**, y es lo más importante para explicar:

1. **Etapa 1 (compilar):** una imagen grande con todas las herramientas de construcción (Maven para el backend, Node para el frontend). Compila el proyecto y produce el resultado (`.jar` o la carpeta `dist`).
2. **Etapa 2 (ejecutar):** una imagen liviana que solo copia ese resultado de la etapa 1. No lleva Maven ni Node.

*Analogía:* la etapa 1 es la cocina con todos los utensilios; la etapa 2 es el plato servido. Al cliente le llevás el plato, no la cocina. La imagen final es mucho más chica.

```dockerfile
# backend/Dockerfile (resumido)
FROM maven:3.9-eclipse-temurin-21 AS compilar   # etapa 1: imagen con Maven y Java
COPY pom.xml .
RUN mvn -q dependency:go-offline                  # baja las dependencias (se guarda en cache)
COPY src ./src
RUN mvn -q package -DskipTests                    # compila y genera el .jar

FROM eclipse-temurin:21-jre                       # etapa 2: solo Java para ejecutar
COPY --from=compilar /app/target/*.jar app.jar    # copia el .jar de la etapa 1
ENTRYPOINT ["java", "-jar", "app.jar"]            # comando al arrancar el contenedor
```

**Truco de la caché:** se copia primero solo el `pom.xml` y se bajan las dependencias, y recién después el código. Docker guarda en caché cada paso: si solo cambiaste código, no vuelve a descargar las dependencias.

El frontend es igual: Node compila con `npm run build` (genera archivos estáticos) y **nginx** (un servidor web liviano) los sirve. Además, `nginx.conf` hace dos cosas:
- Las rutas que no son archivos (como `/monitoreo`) devuelven `index.html`, para que el router de Vue decida qué pantalla mostrar (`try_files`).
- Lo que empieza con `/api/` lo reenvía al backend (`proxy_pass http://backend:8080`). Así el navegador habla siempre con un solo lugar y no hay problemas de CORS.

### Detalles del `docker-compose.yml` (sin magia)
- **`volumes: datos-mysql`**: un *volumen* es una carpeta que Docker guarda fuera del contenedor. Sin él, los datos de MySQL se perderían al borrar el contenedor. Con él se conservan (`docker compose down`) hasta que se borran a propósito (`docker compose down -v`).
- **`depends_on` + `healthcheck`**: MySQL tarda unos segundos en estar listo. El backend espera a que MySQL responda un `ping` antes de arrancar; si no, fallaría al conectarse.
- **`environment`**: las variables de entorno que lee el backend (`DB_URL`, `MAIL_HOST`, etc.). Son las mismas que ya usábamos con `application.properties`, así que no se cambió nada del código.
- **`${DB_PASSWORD:-root1234}`**: "usá la variable `DB_PASSWORD` y, si no está definida, `root1234`". Por eso funciona sin ningún archivo `.env`. Esos valores por defecto son **solo de desarrollo**.
- **`ports: "3000:80"`**: "lo que está en el puerto 80 *dentro* del contenedor, mostralo en el puerto 3000 de *mi* PC".
- **`JAVA_TOOL_OPTIONS: -Duser.timezone=...`**: los contenedores usan hora UTC por defecto; así Java usa la hora de Argentina y las fechas coinciden con la del navegador.

### Cómo usarlo (ejemplos)

```bash
# Levantar todo (la primera vez tarda unos minutos: descarga imágenes y compila)
docker compose up --build

# Ver qué está corriendo
docker compose ps

# Ver el log del backend en vivo
docker compose logs -f backend

# Apagar todo (los datos de la base se conservan)
docker compose down

# Apagar y borrar también los datos
docker compose down -v
```

Cuando termina de arrancar:
- Pantallas: http://localhost:3000 (usuario `admin`, contraseña `demo1234`)
- Swagger: http://localhost:8080/swagger-ui.html
- Mails: http://localhost:8025

**Probar una alarma de punta a punta:** correr la colección de Postman o mandar un dato de carga con temperatura mayor a 40 → aparece un mail en http://localhost:8025 y una alarma en la pantalla "Alarmas".

### Seguridad: qué NO se sube al repo
El archivo `.env` (con contraseñas reales) está en el `.gitignore`. Se sube solo `.env.example`, con valores de ejemplo. Las contraseñas por defecto del compose son únicamente de desarrollo.

---

## 3. Colección de Postman con login

Archivo: `postman/IW3-ordenes-de-carga.postman_collection.json`.

### Qué cambió
- **`00 [Auth] Login`**: hace `POST /auth/login` con el usuario de la variable `usuario` (por defecto `admin`) y **guarda el token** en la variable `token`.
- **Autenticación a nivel colección**: se configuró una sola vez en la colección (*Authorization → Bearer Token → `{{token}}`*). Todos los requests la **heredan**, así que no hubo que tocar ninguno.
- Dos requests de contraste: `00b` (login con contraseña incorrecta → 401) y `00c` (consulta sin token → 401).
- Al final, dos consultas nuevas: `23` Monitoreo y `24` Alarmas.
- Las variables `usuario` y `passwordDemo` se pueden cambiar. Con `admin` se puede usar toda la API; para probar los permisos, cambiar `usuario` a `tms`, `sap`, `carga` u `operador`.

### Cómo se ejecuta
- En Postman: importar el archivo y correr con el **Collection Runner** (el orden importa: el login va primero).
- Por consola: `npx newman run postman/IW3-ordenes-de-carga.postman_collection.json`.
- Resultado verificado contra el sistema levantado con Docker Compose: **31 requests, 59 aserciones, 0 fallos**.

---

## 4. README

Se completó: roles y usuarios de demo, los endpoints nuevos, el despliegue con Docker, el stack, la estructura del repo, la configuración (variables de entorno), cómo probar, y la **checklist del examen final** marcada.

---

## Cómo probarlo

```powershell
# 1. Tests del frontend (incluyen la pantalla de conciliaciones)
cd frontend
npm test
npm run build

# 2. Todo el sistema con Docker
cd ..
docker compose up --build -d

# 3. Postman por consola
npx newman run postman/IW3-ordenes-de-carga.postman_collection.json

# 4. A mano: abrir http://localhost:3000, entrar con admin / demo1234,
#    ir a "Conciliaciones" y hacer click en una orden de la lista.
#    (la lista tiene datos después de correr el paso 3)

# 5. Apagar
docker compose down -v
```

---

## Preguntas posibles del coloquio

**¿Qué es Docker y para qué lo usaron?**
Es una herramienta que empaqueta un programa con todo lo que necesita (una *imagen*) y lo ejecuta aislado (un *contenedor*). Lo usamos para que todo el sistema (base de datos, backend, frontend y servidor de mails) levante con un comando en cualquier computadora, sin instalar Java, Node ni MySQL.

**¿Diferencia entre imagen y contenedor?**
La imagen es el paquete (solo lectura, no corre). El contenedor es esa imagen ejecutándose. De una imagen se pueden crear muchos contenedores.

**¿Qué es Docker Compose?**
Un archivo que describe varios contenedores y cómo se conectan, y los levanta juntos con `docker compose up`. Nosotros tenemos 4 servicios: MySQL, backend, frontend y Mailpit.

**¿Para qué sirve el Dockerfile y por qué tiene dos etapas?**
Es la receta para armar nuestra imagen. Con dos etapas, la primera compila (necesita Maven o Node, que son pesados) y la segunda solo lleva el resultado. La imagen final es mucho más liviana y no contiene herramientas de compilación.

**¿Cómo sabe el backend dónde está la base de datos dentro de Docker?**
Compose crea una red en la que cada servicio se encuentra por su nombre. Por eso `DB_URL` apunta a `mysql:3306`. El nombre `mysql` es el del servicio en el `docker-compose.yml`.

**¿Qué pasa con los datos de MySQL si apago los contenedores?**
Se guardan en un *volumen* (`datos-mysql`), una carpeta que Docker mantiene fuera del contenedor. `docker compose down` los conserva; `docker compose down -v` los borra.

**¿Por qué el backend espera a MySQL (`healthcheck`)?**
MySQL tarda unos segundos en aceptar conexiones. Sin la espera, el backend intentaría conectarse antes y fallaría al arrancar.

**¿Para qué sirve nginx en el frontend?**
Sirve los archivos estáticos que genera `npm run build` y reenvía lo que empieza con `/api` al backend. Así el navegador habla siempre con un único origen (sin CORS), y las rutas de Vue (`/monitoreo`) funcionan aunque se recargue la página.

**¿Qué es Mailpit y por qué no un mail real?**
Es un servidor de correo de prueba: recibe los mails por SMTP y los muestra en una web. Lo usamos para ver las alarmas sin mandar nada a Internet ni configurar una cuenta real.

**¿Cómo evitaron subir contraseñas al repo?**
El `.env` está en el `.gitignore` y solo se sube `.env.example`. Los valores por defecto del compose son de desarrollo y se pueden pisar con variables de entorno. En producción habría que definir `DB_PASSWORD`, `JWT_SECRET` y `PASSWORD_DEMO` con valores propios.

**¿Cómo funciona el login en Postman?**
Un request hace el login y un script de test guarda el token en una variable de la colección (`pm.collectionVariables.set("token", ...)`). La autenticación *Bearer* está configurada una sola vez a nivel colección, con `{{token}}`, y todos los requests la heredan.

**¿Por qué la conciliación no se guarda en la base?**
Se puede calcular siempre a partir de datos que ya están (tara, pesaje final, última masa y los promedios del detalle). Si se guardara, habría que mantenerla sincronizada. Calculándola no puede quedar desactualizada.

**¿Cómo se calcula la diferencia de la conciliación?**
Neto por balanza (pesaje final − tara) menos el producto cargado que midió el caudalímetro. Si es 0, la balanza y el caudalímetro coinciden; si no, esa diferencia es lo que hay que investigar.

**¿Por qué solo se listan órdenes en estado 4?**
Porque la conciliación solo existe cuando hay pesaje final. El backend lo exige (responde 409 si la orden no está finalizada) y la pantalla pide solamente las finalizadas con `?estado=FINALIZADA`.
