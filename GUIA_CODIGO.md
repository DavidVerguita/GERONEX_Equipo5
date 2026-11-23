# GERONEX: guía del código (módulo por módulo)

> Qué hace cada carpeta y cada clase, y **cómo funcionan por dentro las funciones importantes**.
> Para el panorama general (arquitectura, flujo, preguntas) lean primero **GUIA_PROYECTO.md**.

Índice: [1. Backend](#1-backend-spring-boot) · [2. Web](#2-web-htmlcssjs) · [3. Android](#3-android-java) ·
[4. Base de datos](#4-base-de-datos-scripts-sql) · [5. Pruebas](#5-pruebas) · [6. Scripts](#6-scripts-de-arranque-y-azure)

---

## 1. Backend (Spring Boot)

Carpeta: `GERONEX_Equipo5/geronex-backend/src/main/java/com/geronex/backend/`

### Cómo está organizado
Cada **módulo** (paquete) tiene la misma forma, en capas:

```
Controller  ->  Service  ->  Repository  ->  Entidad (tabla)
(HTTP)          (reglas)     (consultas)     (@Entity)
```

- **Entidad** (`@Entity`): una clase = una tabla. Ej. `Reservacion` ↔ tabla `reservacion`.
- **Repository**: interfaz que extiende `JpaRepository`. Spring escribe las consultas solo, a partir del
  nombre del método (`findByEstatusOrderByFechaCreacionAsc` → `WHERE estatus = ? ORDER BY fecha_creacion`).
- **Service** (`@Service`): las reglas de negocio. Sus métodos llevan `@Transactional`: si algo falla a la
  mitad, MySQL deshace todo.
- **Controller** (`@RestController`): traduce HTTP ↔ Java. Recibe JSON, llama al service y regresa JSON.
- **DTO** (paquete `dto`): los objetos que entran y salen por la API (*records* de Java). Llevan las
  validaciones (`@NotBlank`, `@Size`, `@Future`, `@Min`, `@Max`, `@Pattern`). Así nunca se expone una
  entidad completa (por ejemplo, el hash de la contraseña nunca sale).

`@AuthenticationPrincipal Jwt jwt` en un controller = "dame el token ya validado de quien hizo la petición".

### `GeronexApplication`
El `main`. Arranca Spring Boot, que levanta Tomcat en el puerto 8080.

### Paquete `config` → `SecurityConfig`
El portero de la API.
- **`filtroDeSeguridad(http)`**: la lista de reglas, ruta por ruta. Ejemplos:
  - `POST /api/auth/registro` y `/api/auth/login`, y `GET` del catálogo → `permitAll()` (públicas).
  - `/api/admin/**` → `hasRole(ADMINISTRADOR)`.
  - `POST /api/cotizaciones` → solo `AGENTE`; `POST /api/cotizaciones/*/aceptar` → solo `CLIENTE`.
  - Lo demás → `authenticated()` (cualquier sesión válida).
  También desactiva CSRF (no hay cookies) y pone la API en modo **STATELESS** (sin sesiones).
- **`sinSesion` / `sinPermiso`**: escriben el 401 y el 403 con el mismo JSON que los demás errores.
- **`convertidorJwt()`**: toma el claim `rol` del token y lo convierte en la autoridad `ROLE_<rol>`, que es
  lo que revisa `hasRole(...)`.
- **`passwordEncoder()`**: BCrypt.
- **`claveJwt(secreto)`**: lee `JWT_SECRET`; si tiene menos de 32 caracteres, **no arranca** (una clave
  corta sería insegura para HS256).
- **`jwtEncoder` / `jwtDecoder`**: firman y verifican tokens con esa clave (HS256).
- **`corsConfigurationSource(origenes)`**: qué páginas web pueden llamar a la API (`CORS_ORIGENES`).

### Paquete `auth` → registro e inicio de sesión
- **`AuthController`**: `POST /api/auth/registro` y `POST /api/auth/login`.
- **`AuthService.registrar()`**: siempre crea un **CLIENTE** (aunque manden otro rol).
- **`AuthService.crearCuenta(usuario, correo, contraseña, rol, nombre)`**: la usan el registro y el admin.
  1. Limpia los datos (quita espacios, correo en minúsculas).
  2. Rechaza contraseñas de más de 72 *bytes* (BCrypt ignora lo que pase de ahí).
  3. Revisa que el usuario y el correo no existan → si existen, **409**.
  4. Busca el rol, guarda la contraseña con `passwordEncoder.encode()` (hash BCrypt).
  5. Crea el `Perfil` (si no hay nombre, usa el nombre de usuario) y guarda todo de una vez (cascade).
- **`AuthService.iniciarSesion()`**:
  1. Busca al usuario. Si no existe, compara contra un **hash de relleno** para tardar lo mismo
     (así no se puede adivinar qué usuarios existen midiendo el tiempo).
  2. Si no existe, la contraseña no coincide o la cuenta no está ACTIVA → **el mismo** 401
     "Usuario o contraseña incorrectos".
  3. Si todo bien: devuelve `{token, tipo:"Bearer", expiraEnSegundos, usuario}`.
- **`JwtService.generarToken(usuario)`**: arma el token con `issuer`, fecha de emisión, vencimiento
  (60 min), `sub` = id del usuario y `rol`. Lo firma con HS256.

### Paquete `usuario` → mi cuenta
- **`Usuario`, `Perfil`**: entidades (`usuario` 1 a 1 `perfil`). Constantes de estatus `ACTIVO`, `INACTIVO`, `BLOQUEADO`.
- **`UsuarioActual.obtener(jwt)`**: la función que **todos** los services usan primero. Toma el id del token
  (`sub`), busca la cuenta y, si ya no existe o no está ACTIVA, responde 401. Así un usuario desactivado
  pierde el acceso al momento, aunque su token siga vigente.
  También tiene `esCliente()` y `nombreVisible()` (nombre del perfil o, si está vacío, el usuario).
- **`UsuarioController`**: `GET/PUT /api/usuarios/me` y `PATCH /api/usuarios/me/password`.
- **`UsuarioService.actualizarCuenta()`**: cambia correo y nombre; **si la cuenta es de un agente, también
  actualiza su ficha en `agente`** (para que no queden distintos).
- **`UsuarioService.cambiarPassword()`**: verifica la contraseña actual con BCrypt antes de guardar la nueva.

### Paquete `rol`
- **`Roles`**: constantes con los 7 nombres de rol (para no escribir textos sueltos).
- **`Rol`, `RolRepository`**: la tabla `rol`.

### Paquete `catalogo` → catálogo público
- Entidades: `CategoriaServicio`, `Proveedor`, `Servicio` (precio, unidad, destino, estatus) y
  `Disponibilidad` (cupo total y disponible).
- **`Disponibilidad.descontar(n)` / `liberar(n)`**: restan o suman cupo. `liberar` nunca pasa del cupo total;
  que no baje de 0 lo garantiza `ReservacionService`, que revisa el cupo antes de descontar.
- **`DisponibilidadRepository.bloquearPorServicio(id)`**: consulta con `@Lock(PESSIMISTIC_WRITE)` →
  MySQL hace `SELECT … FOR UPDATE`: nadie más puede tocar esa fila hasta que termine la transacción.
- **`CatalogoService.servicios(categoria, texto)`**:
  1. Arma un mapa `idServicio → cupo disponible`.
  2. Trae los servicios activos y filtra por categoría y por texto.
  3. **`normalizar()`** quita acentos y pasa a minúsculas ("Cancún" → "cancun"), así la búsqueda encuentra
     aunque escribas sin acento.
- **`CatalogoController`**: `GET /api/categorias`, `/api/servicios?categoria=&texto=`, `/api/servicios/{id}`.

### Paquete `agente` → agentes
- **`Agente`**: ficha del agente (1 a 1 con su cuenta), estatus ACTIVO/INACTIVO.
- **`AgenteService.actualizar()`**: cambia nombre, correo y teléfono (revisa que el correo no se repita).
- **`AgenteService.cambiarEstatus()`**: baja lógica. Si queda INACTIVO, **también desactiva su cuenta**
  (ya no puede iniciar sesión); si se reactiva, la cuenta vuelve a ACTIVO.
- **`AgenteController`**: `GET /api/agentes` (admin y supervisor), `PUT` y `PATCH …/estatus` (solo admin).

### Paquete `admin` → administración de cuentas
- **`AdminService.crear()`**: usa `crearCuenta()` con el rol que elija el admin. **Si el rol es AGENTE,
  también crea la fila en `agente`.**
- **`AdminService.cambiarEstatus()`**: activa, inactiva o bloquea cuentas. El admin **no puede cambiar su
  propio estatus** (para no dejar el sistema sin administrador). Mantiene igual el estatus del agente.
- **`AdminController`**: `GET/POST /api/admin/usuarios`, `PATCH /api/admin/usuarios/{id}/estatus`.

### Paquete `operacion` → el corazón del negocio
Entidades: `SolicitudViaje`, `Cotizacion`, `DetalleCotizacion`, `Reservacion` (cada una con sus constantes de estatus).

**`SolicitudService`**
- `crear()`: valida que la fecha de regreso no sea antes de la ida (que la ida sea futura lo valida `@Future`
  en el DTO) y guarda la solicitud como **NUEVA**.
- `mias()`: las del cliente del token. `listar(estatus)`: la bandeja de empleados (filtro opcional).
- `obtener(id)`: usa `Acceso.verificarDuenoOEmpleado()`.

**`CotizacionService`** (la más importante)
- **`crear(jwt, datos)`**: lo que pasa cuando el agente pulsa *Enviar cotización*:
  1. Verifica que quien la manda sea un **agente con ficha ACTIVA** (si no, 403).
  2. **Bloquea** la solicitud (`solicitudRepository.bloquear`, FOR UPDATE) para que dos agentes no la
     coticen al mismo tiempo.
  3. Revisa que la solicitud esté **NUEVA** y que el viaje no haya empezado.
  4. Recorre los servicios pedidos: sin repetidos, que existan y estén activos, y que haya **cupo suficiente**.
  5. **Calcula** `subtotal = precio del catálogo × cantidad` y suma el **total**. Guarda el precio del momento
     en cada `DetalleCotizacion`.
  6. Guarda la cotización **ENVIADA**, asigna el agente a la solicitud y la pasa a **COTIZADA**.
- **`aceptar(jwt, id)`**: revisa que sea del cliente y siga ENVIADA, y que el viaje no haya empezado; pone
  cotización y solicitud en **ACEPTADA** y crea la `Reservacion` **POR_VALIDAR** con `folioGenerador.nuevo()`.
- **`rechazar(jwt, id)`**: cotización **RECHAZADA**, solicitud de vuelta a **NUEVA**.

**`ReservacionService`**
- **`confirmar(jwt, id)`**:
  1. Bloquea la reservación y revisa que siga **POR_VALIDAR** y que el viaje no haya empezado.
  2. Ordena los servicios por id y **bloquea su disponibilidad en ese orden** (siempre el mismo orden →
     dos confirmaciones no se traban entre sí).
  3. Si a uno no le alcanza el cupo → 409 y **no se descuenta nada** (la transacción se deshace).
  4. Si alcanza: `disp.descontar(cantidad)` en cada uno, estatus **CONFIRMADA**, guarda quién y cuándo.
- **`rechazar(jwt, id, motivo)`**: **RECHAZADA** con motivo; la solicitud vuelve a **NUEVA** para el agente.
- **`cancelar(jwt, id)`**: revisa dueño, que el estatus sea cancelable y que esté **en tiempo**. Si estaba
  CONFIRMADA, **devuelve el cupo** (`liberar`). Guarda el cargo con `ReglasCancelacion.cargo()`.

**`ReglasCancelacion`** (las reglas acordadas, en un solo lugar)
- `DIAS_ANTICIPACION = 3`, `PORCENTAJE_CARGO = 0.05`.
- `fechaLimite(inicio)` = inicio − 3 días. `enTiempo(inicio, hoy)` = hoy no pasa de la fecha límite.
- `cargo(estatusAntes, total)`: 5% redondeado a centavos si estaba CONFIRMADA; 0 si no.

**`FolioGenerador.nuevo()`**: `GX-` + fecha `yyMMdd` + 4 caracteres al azar con `SecureRandom`, de un alfabeto
sin 0/O/1/I/L. Repite mientras el folio ya exista.

**`Acceso`**: `verificarDuenoOEmpleado()` (el cliente solo ve lo suyo, los empleados todo) y `verificarDueno()`
(para aceptar, rechazar y cancelar). Si no es suyo → **404** (no revela que existe).

**`Mapeador`**: convierte entidades en DTO de respuesta. Por ejemplo, la respuesta de una reservación incluye
`fechaLimiteCancelacion` y `puedeCancelar` (calculados con `ReglasCancelacion`), y la de una solicitud incluye
sus cotizaciones con detalles y, si existe, el folio y estatus de la reservación.

### Paquete `error`
- **`ApiException`**: excepción con código HTTP + mensaje. Los services hacen `throw new ApiException(409, "…")`.
- **`ManejadorErrores`** (`@RestControllerAdvice`): atrapa las excepciones y responde siempre
  `{estado, mensaje, errores, fecha}`:
  - `ApiException` → su código y mensaje.
  - Validación (`@Valid`) → 400 con `errores` por campo (ej. `{"numPersonas":"El máximo es 50…"}`).
  - JSON mal formado → 400. Id que no es número → 400.
  - Bloqueo de concurrencia → 409 "Inténtalo de nuevo". Valor repetido en un `UNIQUE` → 409.
- **`ErrorRespuesta`**: el record con ese formato.

### Paquetes `salud` y `util`
- **`SaludController` → `GET /api/salud`**: responde si la base contesta (nombre de la base, versión de MySQL,
  número de roles). Sirve para comprobar rápido que todo está encendido.
- **`ContrasenaUtil.excedeLimiteBcrypt()`**: true si la contraseña pasa de 72 bytes.

### `application.properties`
Puerto 8080, conexión a MySQL (usuario y contraseña desde `DB_USER` y `DB_PASSWORD`), `ddl-auto=validate`,
`JWT_SECRET`, 60 minutos de token y `CORS_ORIGENES` (si no se define, acepta IntelliJ 63342 y el 8090).

---

## 2. Web (HTML/CSS/JS)

Carpeta: `GERONEX_Equipo5/`. Sin frameworks: HTML + CSS + JavaScript puro.

### Cómo está organizada
- Cada página tiene **3 partes**: `pagina.html` (estructura), `css/pagina.css` (estilos propios) y
  `js/pagina.js` (lógica propia).
- Lo compartido: `css/geronex.css` (paleta, botones, tarjetas, tablas, badges) y `js/geronex.js` (utilidades).
- Orden de carga en cada HTML: primero `js/geronex.js`, después `js/pagina.js`.

### `js/geronex.js` (utilidades compartidas)
- **`API`**: la dirección de la API. Si la página se abrió desde IntelliJ (puerto 63342) usa
  `http://localhost:8080/api`; en cualquier otro caso (servidor 8090 o Azure) usa `/api` del mismo sitio.
- **`Sesion`**: `guardar(token, usuario)`, `token()`, `usuario()`, `rol()`, `cerrar()`. Usa
  **`sessionStorage`** (cada pestaña tiene su sesión).
- **`api(ruta, opciones)`** (la función más usada):
  1. Pone `Content-Type: application/json` y, si hay sesión, `Authorization: Bearer <token>`
     (con `{publica:true}` no manda token, para el catálogo).
  2. Hace el `fetch`. Si no hay conexión, lanza "No se pudo conectar con el servidor".
  3. Si la respuesta es error: si fue 401, cierra la sesión; lanza `{estado, mensaje, errores}`.
  4. Si todo bien, regresa el JSON.
- **`enviar(ruta, metodo, cuerpo)`**: atajo para POST/PUT/PATCH con cuerpo JSON.
- **`textoError()`, `mostrarMensaje()`, `manejarError()`**: muestran el error; si es 401 mandan al login.
- **`PAGINAS_POR_ROL`, `inicioPorRol(rol)`**: a qué página va cada rol.
- **`requerirRol(...roles)`**: protege una página: sin sesión → login; con otro rol → su página.
- **`pintarNav()`**: la barra de arriba según el rol (sus páginas, "Mi cuenta", "Cerrar sesión").
- **`toast()`** (aviso flotante), **`preguntar()`** (diálogo propio de confirmación, en lugar de `confirm`).
- **`esc(valor)`**: escapa `< > & " '` antes de meter texto en el HTML → evita **XSS**.
- Formato: `dinero()` (MXN), `fecha()`, `fechaHora()`, `badge(estatus)`, `nochesEntre()`, `diasPara()`.
- **`imagenDestino(destino)`**: elige la ilustración (`img/destinos/*.svg`) según el destino.
- **`refrescarCada(segundos, fn)`**: repite `fn` cada N segundos (se pausa si la pestaña está oculta y se
  actualiza al volver). Es lo que hace que la bandeja del agente se actualice sola cada 10 s.

### Páginas
| Página | JS | Qué hace | Funciones clave |
|---|---|---|---|
| `index.html` | `index.js` | Catálogo público: buscador, destinos populares, filtros por tipo, precio y orden, detalle de servicio | `cargarServicios()`, `filtrados()`, `pintarCatalogo()`, `abrirDetalle()`, `irASolicitar()` |
| `login.html` | `login.js` | Iniciar sesión o crear cuenta (pestañas) | `handleLogin()`, `handleRegister()`, `iniciarSesion()`, `destinoTras(rol)` |
| `cuenta.html` | `cuenta.js` | Ver y editar mi cuenta, cambiar contraseña | `cargar()`, `pintarCuenta()` |
| `cliente.html` | `cliente.js` | Mis viajes: nueva solicitud, solicitudes con cotizaciones (aceptar o rechazar), reservaciones | `cargar()`, `htmlSolicitud()`, `htmlCotizacion()`, `htmlPasos()` (progreso en 4 pasos) |
| `agente.html` | `agente.js` | Bandeja de solicitudes y armado de cotización | `cargarLista()`, `abrir()`, `cantidadSugerida()`, `pintarPartidas()`, `pintarTotal()`, `enviarCotizacion()` |
| `reservaciones.html` | `reservaciones.js` | Bandeja del responsable: cupo por servicio, confirmar o rechazar con motivo | `cargar()`, `htmlReservacion()`, `pedirMotivo()` |
| `admin.html` | `admin.js` | Resumen, crear cuentas, agentes y cuentas (activar o desactivar) | `cargar()`, `pintarCuentas()`, `pintarAgentes()`, `pintarResumen()` |
| `comprobante.html` | `comprobante.js` | Comprobante de reservación imprimible; cancelar | `pintar()`, `avisoEstatus()`, `cancelarReservacion()` |
| `cotizacion.html` | `cotizacion.js` | Comprobante de cotización imprimible | `pintar()`, `aviso()` |

Detalles que vale la pena contar:
- **`cantidadSugerida(servicio)`** (agente): si el servicio se cobra "por noche", sugiere las noches del viaje;
  si es "por persona", el número de personas; si no, 1. `pintarTotal()` dibuja una barra contra el presupuesto
  del cliente. Pero **el total que vale es el que calcula el backend**.
- **`enviarCotizacion()`**: pide confirmación, manda solo `{idSolicitud, notas, detalles:[{idServicio,cantidad}]}`
  (sin precios) y recarga la bandeja.
- En `index.js`, si alguien sin sesión pulsa "Solicitar", se guarda el destino y, después del login,
  `destinoTras()` lo lleva a `cliente.html?destino=…` con el formulario ya lleno.

### `servidor_web.mjs` (Node, opcional)
Servidor pequeño para abrir la web sin IntelliJ (`1_abrir_web.cmd`): sirve los archivos de la carpeta en el
puerto 8090 y reenvía `/api/*` al backend (8080). Como página y API salen del mismo origen, no hace falta CORS.
No sirve archivos ocultos ni la carpeta del backend.

---

## 3. Android (Java)

Carpeta: `GeronexAndroid/app/src/main/java/com/example/geronex/`. Solo pantallas del **cliente**
(los empleados usan la web).

### Clases base
- **`Config.BASE_URL`**: sale de `BuildConfig` según la variante que se compile (`app/build.gradle.kts`):
  - `debug` → `http://localhost:8080/api` (teléfono por USB con `adb reverse tcp:8080 tcp:8080`).
  - `azure` → `http://20.69.91.86/api` (la VM).
- **`ApiClient`** (el equivalente de `api()` de la web):
  - `llamar(contexto, metodo, ruta, cuerpo, respuesta)` para respuestas objeto y `llamarLista(...)` para arreglos.
  - Arma la petición con **OkHttp** (se usa porque `HttpURLConnection` no soporta PATCH), agrega
    `Authorization: Bearer <token>` (menos en `/auth/*`) y la manda **en segundo plano** (`enqueue`).
  - Cuando llega la respuesta, regresa al **hilo principal** con un `Handler(Looper.getMainLooper())`, así la
    pantalla se puede tocar directamente en `onExito` / `onError`.
  - Si es 401, borra la sesión. `textoError()` saca el mensaje del JSON de error (igual que la web).
  - Estado 0 = no hubo conexión ("Revisa que el backend esté encendido y el teléfono conectado").
- **`SesionStore`**: guarda la sesión en `SharedPreferences`, pero el token **cifrado**:
  - `llave()`: crea (una vez) una llave **AES** dentro del **Android Keystore** (no se puede sacar del teléfono).
  - `guardar()`: cifra el token con **AES/GCM** y guarda el texto cifrado y el IV.
  - `obtenerToken()`: lo descifra; si falla, regresa null (como si no hubiera sesión).
  - `obtenerUsuario()`, `haySesion()`, `cerrar()`.
- **`BaseActivity`** (todas las pantallas heredan de aquí):
  - `cargar()` se llama en `onResume` (al abrir y al regresar a la pantalla); si `segundosRefresco()` > 0,
    se repite sola (Mis viajes: cada 15 s) y se detiene en `onPause`.
  - `exigirCliente()`: sin sesión → login; si la cuenta no es CLIENTE → aviso "Esta sección es para clientes".
  - `manejarError()`: 401 → "Tu sesión venció" y al login; otros errores se muestran.
  - Ayudas de interfaz: `configurarEncabezado()`, `badge()`, `chip()`, `confirmar()` (diálogo), `filaDato()`,
    `imagenDestino()`.
- **`Validador`**: valida campos antes de mandarlos (mismas reglas que la API: usuario ≤ 15, correo,
  contraseña de 8 a 50…).
- **`Formato`**: dinero, fechas (con `Calendar`, porque minSdk 24 no tiene `java.time` completo), textos y
  colores de cada estatus.
- **`Destinos`**: qué imagen (`res/drawable/dest_*.xml`) le toca a cada destino.

### Pantallas
| Pantalla | Qué hace | Llamadas a la API |
|---|---|---|
| `LoginActivity` (inicio) | Iniciar sesión | `POST /auth/login` |
| `RegisterActivity` | Crear cuenta y entrar | `POST /auth/registro`, `POST /auth/login` |
| `MainActivity` | Resumen, aviso "tienes una cotización por responder", accesos, buscador, destinos populares | `GET /solicitudes/mias`, `/reservaciones/mias`, `/servicios` |
| `CatalogoActivity` | Catálogo con búsqueda, chips por tipo, destino, orden, "solo con lugares" y detalle | `GET /servicios` |
| `NuevaSolicitudActivity` | Solicitud con DatePicker (ida desde mañana), personas 1-50, presupuesto y necesidades | `POST /solicitudes` |
| `MisViajesActivity` | Solicitudes con progreso en 4 pasos; aceptar o rechazar; cotizaciones anteriores | `GET /solicitudes/mias`, `POST /cotizaciones/{id}/aceptar` o `/rechazar` |
| `ReservacionesActivity` | Reservaciones con búsqueda y filtros por estatus | `GET /reservaciones/mias` |
| `ComprobanteActivity` | Comprobante; cancelar (solo si `puedeCancelar`), compartir y copiar folio | `GET /reservaciones/{id}`, `POST …/cancelar` |
| `CuentaActivity` | Mi cuenta y cambio de contraseña | `GET/PUT /usuarios/me`, `PATCH /usuarios/me/password` |

`res/xml/network_security_config.xml`: Android bloquea HTTP sin cifrar; este archivo lo permite **solo** hacia
`localhost`, `127.0.0.1`, `10.0.2.2` (emulador) y la IP de Azure.

---

## 4. Base de datos (scripts SQL)

Carpeta `db/` (se corren en orden):
1. `01_esquema_cuentas.sql`: crea la base `geronex` y las tablas `rol`, `usuario`, `perfil`, e inserta los 7 roles.
2. `02_usuario_aplicacion.sql`: crea el usuario MySQL `geronex_app` con SELECT, INSERT y UPDATE (sin DELETE).
3. `03_esquema_negocio.sql`: catálogo, agente, solicitudes, cotizaciones y reservaciones (llaves foráneas, UNIQUE, CHECK).
4. `04_datos_ejemplo.sql`: categorías, proveedores, servicios y su disponibilidad.
5. `05_primer_administrador.sql`: convierte la cuenta `admin` (registrada antes por la API) en ADMINISTRADOR.

---

## 5. Pruebas

Carpeta `geronex-backend/pruebas/` (con el backend encendido):
- `node prueba_api.mjs` → **92 pruebas**: registro y login, catálogo, solicitudes, cotizaciones, aceptar y
  rechazar, confirmar y rechazar reservaciones, cancelar (con y sin cargo) y seguridad (401, 403, 404).
  Crea cuentas `qa_…` con datos de prueba.
- `node prueba_agente.mjs` → **24 pruebas** del módulo Agente (alta, consulta, cambios, baja lógica y reactivación).
- `PRUEBAS.md` y los `.json`: pruebas manuales con `curl.exe`.

---

## 6. Scripts de arranque y Azure

| Archivo | Para qué |
|---|---|
| `1_abrir_web.cmd` | Doble clic: abre la web en http://localhost:8090 (con `servidor_web.mjs`) |
| `2_conectar_telefono.cmd` | Doble clic: `adb reverse` y elegir qué APK instalar (local o Azure) |
| `azure/subir_a_azure.ps1` | Desde la laptop: compila el JAR, junta web + SQL y lo instala en la VM por SSH |
| `azure/instalar_en_vm.sh` | Dentro de la VM: instala Java 21, MySQL y nginx, crea la base, el servicio `geronex` y las cuentas de demo |

Cómo usarlos: **COMO_ENCENDER.md** (local) y **GUIA_AZURE.md** (nube).
