# GERONEX: cómo funciona todo (guía para exponer y defender el proyecto)

> Léanla los tres antes de la exposición. Está escrita para que cualquiera del equipo pueda
> explicar cualquier parte. La presentación es `docs/Presentacion_GERONEX_Final.pptx`
> (19 diapositivas; lo que hay que decir está en las **notas del orador** de cada una).
> La guía función por función está en **GUIA_CODIGO.md**, y la de Azure en **GUIA_AZURE.md**.

---

## 1. En una frase

GERONEX es un sistema para **Iamonos**, una agencia de viajes **asistida**: el cliente pide su viaje
(en la web o en la app Android), un **agente** le arma una cotización con servicios del catálogo,
el cliente la acepta, un **responsable** confirma que haya lugares, y el cliente recibe su
**comprobante** con folio. Todo pasa por **una sola API** (Spring Boot) que guarda en **una sola base MySQL**.

---

## 2. Las piezas (arquitectura)

```
   [ Navegador: web HTML/CSS/JS ]          [ Teléfono: app Android (Java) ]
                 \                                   /
                  \   HTTP + JSON + token JWT       /
                   v                               v
          +------------------------------------------------+
          |  API REST - Spring Boot 4 (Java 21)            |
          |   Seguridad  -> revisa el token y el rol       |
          |   Controller -> recibe la petición             |
          |   Service    -> reglas de negocio              |
          |   Repository -> JPA / Hibernate                |
          +------------------------------------------------+
                               |
                               v
                     [ MySQL 8 - base "geronex" ]
```

| Pieza | Qué es | Dónde está |
|---|---|---|
| Web | 9 páginas HTML. Cada una tiene su `css/<página>.css` y su `js/<página>.js`; lo compartido está en `css/geronex.css` y `js/geronex.js` | `GERONEX_Equipo5/` |
| App Android | Pantallas del **cliente** en Java | `GeronexAndroid/` |
| API | Spring Boot: reglas, seguridad y acceso a datos | `GERONEX_Equipo5/geronex-backend/` |
| Base de datos | MySQL 8, 12 tablas | scripts en `db/` |
| Azure | VM Ubuntu con nginx + backend + MySQL | scripts en `azure/` |

**Regla de oro:** la web y la app **nunca** se conectan a MySQL. Solo hablan con la API. Por eso
las reglas (precios, cupo, cancelación) están en un solo lugar y no se pueden saltar.

**¿Por qué así?** Si mañana se hace una app de iPhone, solo se programa la pantalla: la API y la base
ya existen. Y nadie puede alterar un precio desde el navegador, porque el total lo calcula el servidor.

---

## 3. Cómo viaja una petición (ejemplo: el cliente acepta una cotización)

1. **Clic.** En `cliente.html` el cliente pulsa *Aceptar*. El archivo `js/cliente.js` llama a
   `enviar('/cotizaciones/7/aceptar', 'POST')`.
2. **Petición HTTP.** La función `api()` de `js/geronex.js` hace un `fetch` a
   `http://…/api/cotizaciones/7/aceptar` con la cabecera `Authorization: Bearer <token>`.
   (En Android lo mismo lo hace `ApiClient.llamar(...)` con OkHttp.)
3. **Seguridad.** Spring Security (configurado en `SecurityConfig`) valida la **firma** del token JWT
   y lee el rol. La regla dice que `/api/cotizaciones/*/aceptar` es solo para `CLIENTE`.
   Sin token responde **401**; con otro rol, **403** (siempre en JSON).
4. **Controller.** `CotizacionController` recibe la petición y llama a `CotizacionService.aceptar(jwt, 7)`.
5. **Service (reglas).** Busca la cotización, revisa que **sea de ese cliente** (si no, responde 404),
   que siga **ENVIADA** y que el viaje **no haya empezado**. Luego cambia los estados y crea la
   reservación con un **folio** nuevo (`FolioGenerador`).
6. **Repository + MySQL.** Todo ocurre dentro de una **transacción** (`@Transactional`): o se guarda
   todo, o no se guarda nada.
7. **Respuesta.** La API responde **201** con la reservación en JSON (folio, total, estatus…).
   La página la muestra y, como todas las pantallas consultan la API cada 10 s, el responsable la ve
   aparecer sola en su bandeja.

Cuando algo sale mal, **todos** los errores llegan con el mismo formato:
`{ "estado": 409, "mensaje": "Esta cotización ya fue respondida", "errores": {}, "fecha": "…" }`.
La web y la app muestran el `mensaje` tal cual.

---

## 4. El flujo de negocio paso a paso

| # | Quién | Dónde | Qué pasa | Estados |
|---|---|---|---|---|
| 0 | Cualquiera | Web (`index.html`) o app | Consulta el catálogo **sin cuenta** | – |
| 1 | Cliente | Web (`cliente.html`) o app (`NuevaSolicitudActivity`) | Crea una **solicitud**: destino, fechas, personas (1 a 50), presupuesto y necesidades | Solicitud **NUEVA** |
| 2 | Agente | Web (`agente.html`) | Ve la solicitud en su bandeja, elige servicios y cantidades (el sistema **sugiere** noches o personas) y envía la **cotización**. El **total lo calcula el backend** | Cotización **ENVIADA**, solicitud **COTIZADA** |
| 3a | Cliente | Web o app (`MisViajesActivity`) | **Acepta**: nace la **reservación** con folio `GX-AAMMDD-XXXX` | Cotización **ACEPTADA**, solicitud **ACEPTADA**, reservación **POR_VALIDAR** |
| 3b | Cliente | Web o app | **Rechaza**: la solicitud regresa para que el agente mande otra | Cotización **RECHAZADA**, solicitud **NUEVA** |
| 4a | Responsable de reservaciones (o supervisor) | Web (`reservaciones.html`) | **Confirma**: se valida y **descuenta el cupo** de cada servicio | Reservación **CONFIRMADA** |
| 4b | Responsable | Web | **Rechaza** con un motivo: la solicitud regresa al agente | Reservación **RECHAZADA**, solicitud **NUEVA** |
| 5 | Cliente | Web (`comprobante.html`) o app (`ComprobanteActivity`) | Ve e imprime su **comprobante**; puede **cancelar** | – |
| 6 | Cliente | Web o app | **Cancela** si faltan **3 días o más**. Si estaba CONFIRMADA: cargo del **5%** (solo se registra) y el cupo **regresa**. Si estaba POR_VALIDAR: sin cargo | Reservación **CANCELADA** |

Validaciones de fechas: no se puede **cotizar, aceptar ni confirmar** un viaje cuya fecha de inicio
ya llegó. La fecha de inicio debe ser futura y la de regreso no puede ser antes de la de ida.

---

## 5. Roles

| Rol | Qué puede hacer | Página inicial |
|---|---|---|
| CLIENTE | Pedir viajes, aceptar o rechazar cotizaciones, ver comprobantes y cancelar | `cliente.html` (y la app) |
| AGENTE | Ver solicitudes y cotizar (solo si su ficha de agente está ACTIVA) | `agente.html` |
| RESPONSABLE_RESERVACIONES | Confirmar o rechazar reservaciones | `reservaciones.html` |
| SUPERVISOR | Ver solicitudes, validar reservaciones, consultar agentes | `agente.html` |
| ADMINISTRADOR | Crear cuentas de cualquier rol, activar o desactivar cuentas, gestionar agentes | `admin.html` |
| AUDITOR | Solo consultar bandejas | `agente.html` |
| RESPONSABLE_PROVEEDORES | Existe en la base (siguiente fase) | – |

- El **registro público siempre crea un CLIENTE**. Los empleados los crea el administrador.
- El **primer administrador**: se registra la cuenta `admin` (queda como cliente) y se corre
  `db/05_primer_administrador.sql`, que le cambia el rol.
- El **agente** es una cuenta con rol AGENTE **más** una fila en la tabla `agente`
  (nombre, correo, teléfono, estatus). Al crear la cuenta con rol AGENTE se crean las dos.

---

## 6. Base de datos (12 tablas)

**Cuentas:** `rol` (los 7 roles), `usuario` (nombre de usuario único de máximo 15, correo único,
contraseña en hash BCrypt, rol y estatus ACTIVO/INACTIVO/BLOQUEADO), `perfil` (1 a 1 con usuario: nombre).

**Catálogo:** `categoria_servicio` (HOTEL, VUELO, TRANSPORTE, PAQUETE), `proveedor`, `servicio`
(precio y unidad: por noche, por persona o por viaje), `disponibilidad` (1 a 1 con servicio:
`cupo_total` y `cupo_disponible`).

**Operación:** `agente`, `solicitud_viaje`, `cotizacion`, `detalle_cotizacion` (guarda el
**precio del momento**: si el precio del catálogo cambia, la cotización no cambia) y `reservacion`
(folio único, una por cotización, motivo de rechazo, cargo de cancelación, quién la validó).

Decisiones que conviene saber explicar:
- La app se conecta con el usuario MySQL `geronex_app`, que solo tiene **SELECT, INSERT y UPDATE**.
  **No puede borrar.** Las bajas son **lógicas** (estatus INACTIVO).
- Los estatus se guardan como texto (`VARCHAR`), no como `ENUM`, porque Hibernate valida el esquema
  al arrancar (`ddl-auto=validate`) y con ENUM fallaría.
- `ddl-auto=validate`: Hibernate **no** crea ni cambia tablas; solo revisa que coincidan con las clases.
  El esquema lo controlan los scripts SQL.

---

## 7. Seguridad

| Tema | Cómo se resolvió |
|---|---|
| Inicio de sesión | `POST /api/auth/login` devuelve un **token JWT** firmado con HS256 que dura **60 minutos**. Lleva `sub` (id del usuario) y `rol` |
| Sin sesiones | El servidor no guarda sesiones (*stateless*): cada petición trae su token |
| Contraseñas | Hash **BCrypt** (con sal). De 8 a 50 caracteres. Nunca se guardan en texto |
| Permisos | `SecurityConfig` dice qué rol puede usar cada ruta. 401 sin sesión, 403 sin permiso |
| Datos ajenos | Si un cliente pide una solicitud o reservación que no es suya, la API responde **404** (no revela que existe) |
| Login | Mismo mensaje si el usuario no existe, si la contraseña está mal o si la cuenta está inactiva; además tarda lo mismo (hash de relleno) |
| Cuenta desactivada | Aunque el token no haya vencido, `UsuarioActual` rechaza cuentas que ya no están ACTIVAS |
| Token en la web | `sessionStorage` (por pestaña; se borra al cerrarla) |
| Token en Android | Cifrado con **AES-GCM** con una llave del **Android Keystore** (`SesionStore`) |
| XSS | Todo texto que viene de la API se pinta con `esc()` en la web |
| CORS | Solo los orígenes de `CORS_ORIGENES` pueden llamar a la API desde otra página |
| Secretos | `DB_PASSWORD` y `JWT_SECRET` van en variables de entorno, nunca en el código |
| Concurrencia | Al confirmar o cancelar, las filas de `disponibilidad` se bloquean (`SELECT … FOR UPDATE`) en orden de id: nunca se vende el mismo lugar dos veces y no hay interbloqueos |

Si cambia el rol de alguien en la base, tiene que volver a iniciar sesión (el rol viaja dentro del token).

---

## 8. Guion de la exposición (15 a 20 minutos)

Repártanse así (cámbienlo si quieren): **A** = integrante de base de datos, **B** = integrante de
Android, **David** = backend y web. Las notas de cada diapositiva traen el texto sugerido.

| Diap. | Tema | Quién | Tiempo |
|---|---|---|---|
| 1-2 | Portada y equipo | A | 1 min |
| 3-5 | Problema, propuesta y flujo de negocio | A | 3.5 min |
| 6-7 | Arquitectura y viaje de una petición | David | 3 min |
| 8-9 | Base de datos y estados | A | 2.5 min |
| 10-11 | Seguridad y reglas de negocio | David | 3 min |
| 12-13 | La web y la app Android | David / B | 2 min |
| 14 | **Demo en vivo** | David (laptop) + B (teléfono) | 4-5 min |
| 15 | Azure | David | 1.5 min |
| 16-18 | Pruebas, tecnologías, siguiente fase | B | 2.5 min |
| 19 | Gracias y preguntas | todos | – |

Si van cortos de tiempo: salten la 9 (estados) y la 17 (tecnologías).
Si la demo falla: recarguen la página; si sigue fallando, usen la diapositiva 12 (capturas) y sigan.

**Antes de entrar (checklist):**
1. Backend encendido (en Azure: abrir `http://20.69.91.86/api/salud` y ver `"estado":"OK"`).
2. Tres pestañas abiertas **desde cero** (Ctrl+T, pegar la dirección): `agente1`, `reserva1` y `cliente1`.
3. App instalada en el teléfono con la sesión de `cliente1` (o una cuenta nueva creada ahí mismo).
4. Presentación abierta en modo presentador (para ver las notas).

---

## 9. Preguntas que podría hacer el profe (con respuesta)

**1. ¿Por qué una API y no conectar la web y la app directo a la base?**
Porque así las reglas viven en un solo lugar. Si la app se conectara a MySQL, cualquiera podría sacar
la contraseña de la base del APK y modificar precios o cupos. Además, la web y la app comparten todo:
lo que se hace en una aparece en la otra.

**2. ¿Qué es un JWT y por qué lo usaron?**
Es un texto firmado que el servidor entrega al iniciar sesión. Dice "este es el usuario 5 y es CLIENTE"
y trae una firma que solo el servidor puede generar con su clave secreta. En cada petición se manda
de vuelta; si alguien lo altera, la firma ya no coincide y se rechaza. No hay que guardar sesiones en
el servidor, y funciona igual para la web y para Android.

**3. ¿Cómo guardan las contraseñas?**
Con BCrypt: un hash lento y con sal. No se puede regresar a la contraseña original; al iniciar sesión
se compara el hash. Si roban la base, no se ven las contraseñas.

**4. ¿Qué pasa si dos responsables confirman al mismo tiempo la última habitación?**
Al confirmar, la API bloquea las filas de disponibilidad con `SELECT … FOR UPDATE` dentro de una
transacción. El segundo espera a que termine el primero, ve que ya no hay cupo y recibe un error 409.
Además se bloquean siempre en el mismo orden (por id de servicio) para que no se traben entre sí.

**5. ¿Quién calcula el total de la cotización?**
El backend. El agente solo manda qué servicios y cuántos; el servidor toma el precio del catálogo,
calcula subtotales y total, y guarda el precio del momento en `detalle_cotizacion`.

**6. ¿Cómo funciona la cancelación?**
Se puede cancelar si faltan al menos 3 días naturales para la fecha de inicio. Si ya estaba confirmada,
se registra un cargo del 5% del total (no hay cobro real) y se devuelve el cupo. Si seguía por validar,
no hay cargo porque todavía no se había apartado nada. Está en la clase `ReglasCancelacion`.

**7. ¿Por qué no hay DELETE?**
Por seguridad y por historial. El usuario de MySQL de la app no tiene permiso de borrar. Las bajas son
lógicas: se cambia el estatus a INACTIVO y el registro se conserva (por ejemplo, un agente dado de baja
sigue apareciendo en las cotizaciones viejas).

**8. ¿Es tiempo real? ¿Cómo se entera el agente de una solicitud nueva?**
No es tiempo real: las páginas consultan la API cada 10 segundos (`refrescarCada`) y la app cada 15 en
Mis viajes y al volver a cada pantalla. Para una sola agencia es suficiente; con muchos usuarios se
usarían WebSockets.

**9. ¿Cómo se ven varios roles en la misma computadora?**
La web guarda el token en `sessionStorage`, que es independiente en cada pestaña. Por eso se puede tener
al cliente en una pestaña y al agente en otra.

**10. ¿Qué es MVC en su proyecto?**
Modelo: las entidades JPA (`Usuario`, `Reservacion`…) y los DTO. Vista: la web y la app. Controlador: los
`@RestController` (`CotizacionController`…). Además separamos la lógica en *Services* y el acceso a datos
en *Repositories* (arquitectura en capas).

**11. ¿Cómo lo subieron a Azure?**
Una máquina virtual Ubuntu. Adentro: MySQL, el backend como servicio de systemd (arranca solo si la VM
se reinicia) y nginx en el puerto 80, que sirve la web y reenvía `/api` al backend. En el grupo de
seguridad de red (NSG) solo están abiertos el 22 (SSH) y el 80. El 8080 y el 3306 no se exponen.
Un script desde la laptop compila, copia e instala todo.

**12. ¿Y HTTPS?**
No lo tiene porque es un proyecto escolar sin dominio. Sería lo siguiente: un dominio y un certificado
gratuito de Let's Encrypt en nginx. Por eso la app solo permite HTTP hacia localhost y la IP de la VM.

**13. ¿Cómo probaron que funciona?**
Con un script de Node (`prueba_api.mjs`) que llama a la API encendida y recorre todo: registro, solicitud,
cotización, aceptación, confirmación, rechazo, cancelación y también los casos prohibidos (401, 403, 404).
Pasan 92 de 92. Para el módulo de agente hay 24 pruebas más y 36 casos documentados.

**14. ¿Qué pasa si el token vence?**
Dura 60 minutos. La API responde 401, la web borra la sesión y manda al login; la app también cierra la
sesión, avisa "Tu sesión venció" y regresa a la pantalla de login.

**15. ¿Por qué Spring Boot y no otra cosa?**
Es el estándar en Java para APIs: trae Spring Security (JWT, roles), Spring Data JPA (acceso a datos) y
validación con anotaciones (`@NotBlank`, `@Size`, `@Future`) listos. Y el equipo ya conocía Java por Android.

**16. ¿Qué quedó fuera del alcance?**
Pagos reales, notificaciones por correo, reportes, auditoría, solicitudes de cambio, reasignaciones y
seguimiento de proveedores. Las tablas están en el diagrama ER para la siguiente fase.

**17. ¿Qué pasa si el agente cotiza algo sin cupo?**
La API revisa el cupo disponible al cotizar y responde 409 ("No hay cupo suficiente de…"). El cupo se
descuenta hasta que el responsable confirma, y se vuelve a revisar en ese momento.

**18. ¿Cómo se genera el folio?**
`GX-` + fecha `AAMMDD` + 4 caracteres al azar (sin 0, O, 1, I ni L para que no se confundan). Si ya
existe, se genera otro. En la base es `UNIQUE`.

---

## 10. Cosas que pueden preguntar del código (dónde está cada cosa)

| Si preguntan por… | Abran |
|---|---|
| Rutas y permisos | `config/SecurityConfig.java` |
| Login y registro | `auth/AuthService.java`, `auth/JwtService.java` |
| Crear cotización y calcular total | `operacion/CotizacionService.java` → `crear()` |
| Confirmar y descontar cupo | `operacion/ReservacionService.java` → `confirmar()` |
| Cancelación (3 días, 5%) | `operacion/ReglasCancelacion.java` |
| Folio | `operacion/FolioGenerador.java` |
| Formato de errores | `error/ManejadorErrores.java` |
| Cómo llama la web a la API | `js/geronex.js` → `api()` |
| Cómo llama Android a la API | `ApiClient.java` → `llamar()` |
| Token cifrado en Android | `SesionStore.java` |

Detalle de cada módulo y función: **GUIA_CODIGO.md**.
